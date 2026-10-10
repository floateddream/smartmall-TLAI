package com.smartmall.tlai.ai.gateway;

import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiChatResult;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class SpringAiChatGateway implements AiChatGateway {
    private static final Logger log = LoggerFactory.getLogger(SpringAiChatGateway.class);

    @Override
    public AiChatResult complete(AiModelConfig config, List<AiChatMessage> messages,
                                 String systemPrompt, String apiKey, Duration timeout) {
        long startedAt = System.nanoTime();
        try {
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(timeout);
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(config.baseUrl())
                    .apiKey(apiKey)
                    .completionsPath("/chat/completions")
                    .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                    .build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(config.modelName())
                    .temperature(config.temperature().doubleValue())
                    .maxTokens(config.maxTokens())
                    .build();
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .build();

            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(systemPrompt));
            messages.forEach(message -> promptMessages.add(message.role() == AiMessageRole.USER
                    ? new UserMessage(message.content()) : new AssistantMessage(message.content())));
            String content = model.call(new Prompt(promptMessages)).getResult().getOutput().getText();
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return new AiChatResult(content, config.modelName(), latencyMs);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            // 不记录请求消息和 API Key，只保留定位供应商调用所需的安全元数据。
            log.error("AI 供应商接口调用失败：配置ID={}，供应商={}，模型={}，接口地址={}，耗时毫秒={}",
                    config.id(), config.provider(), config.modelName(), config.baseUrl(), latencyMs, exception);
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR,
                    "AI 服务调用失败，请检查接口地址、模型名称和密钥");
        }
    }
}
