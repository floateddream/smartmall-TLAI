package com.smartmall.tlai.ai.gateway;

import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class SpringAiEmbeddingGateway implements AiEmbeddingGateway {
    private static final Logger log = LoggerFactory.getLogger(SpringAiEmbeddingGateway.class);

    @Override
    public float[] embed(AiModelConfig config, String input, String apiKey, Duration timeout) {
        long startedAt = System.nanoTime();
        try {
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(timeout);
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(config.baseUrl())
                    .apiKey(apiKey)
                    .embeddingsPath("/embeddings")
                    .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                    .build();
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(config.modelName())
                    .dimensions(config.embeddingDimensions())
                    .build();
            OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(api, MetadataMode.NONE, options);
            return model.embed(input);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            // 不记录输入文本和凭据，只保留定位远程服务所需的安全元数据。
            log.error("AI 向量模型调用失败：配置ID={}，供应商={}，模型={}，接口地址={}，耗时毫秒={}",
                    config.id(), config.provider(), config.modelName(), config.baseUrl(), latencyMs, exception);
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR,
                    "Embedding 服务调用失败，请检查接口地址、模型名称和服务状态");
        }
    }
}
