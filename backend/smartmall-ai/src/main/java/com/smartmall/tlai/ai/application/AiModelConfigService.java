package com.smartmall.tlai.ai.application;

import com.smartmall.tlai.ai.config.AiProperties;
import com.smartmall.tlai.ai.gateway.AiChatGateway;
import com.smartmall.tlai.ai.gateway.AiEmbeddingGateway;
import com.smartmall.tlai.ai.model.AiChatMessage;
import com.smartmall.tlai.ai.model.AiCallScene;
import com.smartmall.tlai.ai.model.AiCallStatus;
import com.smartmall.tlai.ai.model.AiMessageRole;
import com.smartmall.tlai.ai.model.AiModelConfig;
import com.smartmall.tlai.ai.model.AiModelType;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.ai.repository.AiModelConfigRepository;
import com.smartmall.tlai.ai.security.AiCredentialCipher;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AiModelConfigService {
    private static final Logger log = LoggerFactory.getLogger(AiModelConfigService.class);
    private static final String TEST_PROMPT = "这是一次模型连通性测试。请只回复 OK。";

    private final AiModelConfigRepository repository;
    private final AiChatGateway gateway;
    private final AiEmbeddingGateway embeddingGateway;
    private final AiProperties properties;
    private final AiCredentialCipher credentialCipher;
    private final AiCallLogService callLogService;

    public AiModelConfigService(AiModelConfigRepository repository, AiChatGateway gateway,
                                AiEmbeddingGateway embeddingGateway,
                                AiProperties properties, AiCredentialCipher credentialCipher,
                                AiCallLogService callLogService) {
        this.repository = repository;
        this.gateway = gateway;
        this.embeddingGateway = embeddingGateway;
        this.properties = properties;
        this.credentialCipher = credentialCipher;
        this.callLogService = callLogService;
    }

    @Transactional(readOnly = true)
    public List<AiModelConfig> listPersonal(long ownerUserId) {
        return repository.findByOwner(ownerUserId);
    }

    @Transactional(readOnly = true)
    public List<AiModelConfig> listSystem() {
        return repository.findSystem();
    }

    @Transactional
    public AiModelConfig createPersonal(AiModelCommand command, long ownerUserId) {
        return create(command, ownerUserId, AiModelType.CHAT);
    }

    @Transactional
    public AiModelConfig createSystem(AiModelCommand command) {
        return create(command, null, AiModelType.EMBEDDING);
    }

    private AiModelConfig create(AiModelCommand command, Long ownerUserId, AiModelType requiredType) {
        String name = requireText(command.name(), "配置名称不能为空");
        if (repository.findByName(name, ownerUserId).isPresent()) {
            throw new BusinessException(ErrorCode.AI_MODEL_NAME_EXISTS);
        }
        AiModelType modelType = requireModelType(command.modelType());
        requireScopeType(modelType, requiredType);
        AiProvider provider = requireProvider(command.provider());
        requireProviderSupports(provider, modelType);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AiModelConfig created = repository.insert(new AiModelConfig(null, name, modelType, provider,
                normalizeBaseUrl(command.baseUrl(), provider), requireText(command.modelName(), "模型名称不能为空"),
                encryptCredential(command.apiKey(), provider),
                requireTemperature(command.temperature()), requireMaxTokens(command.maxTokens()),
                requireEmbeddingDimensions(command.embeddingDimensions(), modelType), false, true,
                ownerUserId, command.operatorId(), command.operatorId(), now, now));
        log.info("AI 模型配置创建成功：配置ID={}，名称={}，类型={}，归属用户ID={}，供应商={}，模型={}，操作人ID={}",
                created.id(), created.name(), created.modelType(), ownerUserId,
                created.provider(), created.modelName(), command.operatorId());
        return created;
    }

    @Transactional
    public AiModelConfig updatePersonal(long id, AiModelCommand command, long ownerUserId) {
        return update(requirePersonalConfig(id, ownerUserId), command, AiModelType.CHAT);
    }

    @Transactional
    public AiModelConfig updateSystem(long id, AiModelCommand command) {
        return update(requireSystemConfig(id), command, AiModelType.EMBEDDING);
    }

    private AiModelConfig update(AiModelConfig current, AiModelCommand command, AiModelType requiredType) {
        long id = current.id();
        String name = requireText(command.name(), "配置名称不能为空");
        repository.findByName(name, current.ownerUserId()).filter(found -> !found.id().equals(id)).ifPresent(found -> {
            throw new BusinessException(ErrorCode.AI_MODEL_NAME_EXISTS);
        });
        AiModelType modelType = requireModelType(command.modelType());
        requireScopeType(modelType, requiredType);
        AiProvider provider = requireProvider(command.provider());
        requireProviderSupports(provider, modelType);
        if (current.active() && current.modelType() != modelType) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前使用中的模型不能修改模型类型，请先激活同类型的其他模型");
        }
        String apiKeyCiphertext = current.apiKeyCiphertext();
        if (command.apiKey() != null && !command.apiKey().isBlank()) {
            apiKeyCiphertext = credentialCipher.encrypt(command.apiKey());
        } else if (provider.credentialRequired()
                && (!current.credentialConfigured() || current.provider() != provider)) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_MISSING,
                    current.provider() != provider ? "切换供应商时必须填写新的 API Key" : "请填写 API Key");
        } else if (!provider.credentialRequired()) {
            apiKeyCiphertext = null;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AiModelConfig updated = repository.update(new AiModelConfig(current.id(), name, modelType, provider,
                normalizeBaseUrl(command.baseUrl(), provider), requireText(command.modelName(), "模型名称不能为空"),
                apiKeyCiphertext,
                requireTemperature(command.temperature()), requireMaxTokens(command.maxTokens()),
                requireEmbeddingDimensions(command.embeddingDimensions(), modelType), current.active(), current.enabled(),
                current.ownerUserId(), current.createdBy(), command.operatorId(), current.createdAt(), now));
        log.info("AI 模型配置更新成功：配置ID={}，名称={}，类型={}，供应商={}，模型={}，密钥是否变更={}，操作人ID={}",
                updated.id(), updated.name(), updated.modelType(), updated.provider(), updated.modelName(),
                command.apiKey() != null && !command.apiKey().isBlank(), command.operatorId());
        return updated;
    }

    @Transactional
    public AiModelConfig activatePersonal(long id, long ownerUserId) {
        return activate(requirePersonalConfig(id, ownerUserId), ownerUserId);
    }

    @Transactional
    public AiModelConfig activateSystem(long id, long operatorId) {
        return activate(requireSystemConfig(id), operatorId);
    }

    private AiModelConfig activate(AiModelConfig config, long operatorId) {
        repository.lockConfiguration();
        if (!config.enabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "停用的模型不能设为当前模型");
        }
        if (config.credentialRequired() && !config.credentialConfigured()) {
            throw new BusinessException(ErrorCode.AI_CREDENTIAL_MISSING, "请先为该模型配置 API Key");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        repository.clearActive(config.modelType(), config.ownerUserId(), now, operatorId);
        repository.updateActive(config.id(), true, now, operatorId);
        AiModelConfig activated = requireConfig(config.id());
        log.info("AI 模型已激活：配置ID={}，类型={}，归属用户ID={}，供应商={}，模型={}，操作人ID={}",
                activated.id(), activated.modelType(), activated.ownerUserId(),
                activated.provider(), activated.modelName(), operatorId);
        return activated;
    }

    @Transactional
    public AiModelConfig changePersonalEnabled(long id, boolean enabled, long ownerUserId) {
        return changeEnabled(requirePersonalConfig(id, ownerUserId), enabled, ownerUserId);
    }

    @Transactional
    public AiModelConfig changeSystemEnabled(long id, boolean enabled, long operatorId) {
        return changeEnabled(requireSystemConfig(id), enabled, operatorId);
    }

    private AiModelConfig changeEnabled(AiModelConfig config, boolean enabled, long operatorId) {
        repository.lockConfiguration();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        repository.updateEnabled(config.id(), enabled, now, operatorId);
        AiModelConfig changed = requireConfig(config.id());
        log.info("AI 模型启用状态已变更：配置ID={}，归属用户ID={}，是否启用={}，操作人ID={}",
                config.id(), config.ownerUserId(), enabled, operatorId);
        return changed;
    }

    @Transactional(readOnly = true)
    public String testPersonal(long id, long ownerUserId) {
        return test(requirePersonalConfig(id, ownerUserId));
    }

    @Transactional(readOnly = true)
    public String testSystem(long id) {
        return test(requireSystemConfig(id));
    }

    private String test(AiModelConfig config) {
        log.info("AI 模型连通性测试开始：配置ID={}，类型={}，供应商={}，模型={}",
                config.id(), config.modelType(), config.provider(), config.modelName());
        long startedAt = System.nanoTime();
        try {
            if (config.modelType() == AiModelType.EMBEDDING) {
                float[] vector = embeddingGateway.embed(config, TEST_PROMPT, resolveApiKey(config), properties.requestTimeout());
                if (vector.length != config.embeddingDimensions()) {
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_DIMENSION_MISMATCH,
                            "配置维度为 %d，但模型实际返回 %d 维".formatted(config.embeddingDimensions(), vector.length));
                }
                long latencyMs = elapsedMs(startedAt);
                recordTestSafely(config, AiCallStatus.SUCCESS, latencyMs, 1, (long) vector.length, null);
                log.info("AI 向量模型连通性测试成功：配置ID={}，供应商={}，模型={}，向量维度={}，耗时毫秒={}",
                        config.id(), config.provider(), config.modelName(), vector.length, latencyMs);
                return "连接成功，返回 %d 维向量，耗时 %d ms".formatted(vector.length, latencyMs);
            }
            var result = gateway.complete(config, List.of(new AiChatMessage(AiMessageRole.USER, TEST_PROMPT)),
                    "你是模型连通性检测助手。", resolveApiKey(config), properties.requestTimeout());
            recordTestSafely(config, AiCallStatus.SUCCESS, elapsedMs(startedAt), 1,
                    (long) result.content().length(), null);
            log.info("AI 对话模型连通性测试成功：配置ID={}，供应商={}，模型={}，耗时毫秒={}",
                    config.id(), config.provider(), config.modelName(), result.latencyMs());
            return result.content();
        } catch (RuntimeException exception) {
            recordTestSafely(config, AiCallStatus.FAILED, elapsedMs(startedAt), 0, null,
                    AiCallLogService.errorCode(exception));
            throw exception;
        }
    }

    private void recordTestSafely(AiModelConfig config, AiCallStatus status, long latencyMs,
                                  int outputCount, Long outputUnitCount, String errorCode) {
        try {
            callLogService.record(config, AiCallScene.MODEL_CONNECTIVITY_TEST, status, latencyMs,
                    1, TEST_PROMPT.length(), outputCount, outputUnitCount, errorCode);
        } catch (RuntimeException exception) {
            log.error("AI 连通性测试日志保存失败：配置ID={}，类型={}，状态={}",
                    config.id(), config.modelType(), status, exception);
        }
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private AiModelConfig requireConfig(long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND));
    }

    private AiModelConfig requirePersonalConfig(long id, long ownerUserId) {
        AiModelConfig config = requireConfig(id);
        if (config.modelType() != AiModelType.CHAT || config.ownerUserId() == null
                || config.ownerUserId() != ownerUserId) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return config;
    }

    private AiModelConfig requireSystemConfig(long id) {
        AiModelConfig config = requireConfig(id);
        if (config.modelType() != AiModelType.EMBEDDING || config.ownerUserId() != null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return config;
    }

    private static void requireScopeType(AiModelType actual, AiModelType required) {
        if (actual != required) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    required == AiModelType.CHAT ? "个人模型只能配置聊天模型" : "系统模型只能配置 Embedding 模型");
        }
    }

    private static AiProvider requireProvider(AiProvider provider) {
        if (provider == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型供应商不能为空");
        }
        return provider;
    }

    private static AiModelType requireModelType(AiModelType modelType) {
        if (modelType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型类型不能为空");
        }
        return modelType;
    }

    private static void requireProviderSupports(AiProvider provider, AiModelType modelType) {
        if (!provider.supports(modelType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "%s 不支持 %s 模型".formatted(provider.displayName(), modelType));
        }
    }

    private String encryptCredential(String apiKey, AiProvider provider) {
        if (apiKey == null || apiKey.isBlank()) {
            if (provider.credentialRequired()) {
                throw new BusinessException(ErrorCode.AI_CREDENTIAL_MISSING, "请填写 API Key");
            }
            return null;
        }
        return credentialCipher.encrypt(apiKey);
    }

    private String resolveApiKey(AiModelConfig config) {
        // Ollama 的 OpenAI 兼容接口要求客户端传一个非空值，但服务端会忽略它。
        return config.credentialRequired() ? credentialCipher.decrypt(config.apiKeyCiphertext()) : "ollama";
    }

    private static String normalizeBaseUrl(String value, AiProvider provider) {
        String candidate = value == null || value.isBlank() ? provider.defaultBaseUrl() : value;
        String baseUrl = requireText(candidate, "接口地址不能为空");
        try {
            URI uri = URI.create(baseUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接口地址必须是有效的 HTTP 或 HTTPS URL");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static BigDecimal requireTemperature(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(2)) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "温度参数必须在 0 到 2 之间");
        }
        return value;
    }

    private static int requireMaxTokens(int value) {
        if (value < 128 || value > 32768) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最大输出 Token 必须在 128 到 32768 之间");
        }
        return value;
    }

    private static Integer requireEmbeddingDimensions(Integer value, AiModelType modelType) {
        if (modelType == AiModelType.CHAT) {
            return null;
        }
        if (value == null || value < 1 || value > 4096) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding 向量维度必须在 1 到 4096 之间");
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
