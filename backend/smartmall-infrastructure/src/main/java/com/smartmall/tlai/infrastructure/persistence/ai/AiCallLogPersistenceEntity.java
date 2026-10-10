package com.smartmall.tlai.infrastructure.persistence.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("ai_call_log")
public class AiCallLogPersistenceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long modelConfigId;
    private String modelType;
    private String provider;
    private String modelName;
    private String scene;
    private String status;
    private Long latencyMs;
    private Integer inputCount;
    private Long inputCharacterCount;
    private Integer outputCount;
    private Long outputUnitCount;
    private String errorCode;
    private String traceId;
    private Long conversationId;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Integer getInputCount() { return inputCount; }
    public void setInputCount(Integer inputCount) { this.inputCount = inputCount; }
    public Long getInputCharacterCount() { return inputCharacterCount; }
    public void setInputCharacterCount(Long inputCharacterCount) { this.inputCharacterCount = inputCharacterCount; }
    public Integer getOutputCount() { return outputCount; }
    public void setOutputCount(Integer outputCount) { this.outputCount = outputCount; }
    public Long getOutputUnitCount() { return outputUnitCount; }
    public void setOutputUnitCount(Long outputUnitCount) { this.outputUnitCount = outputUnitCount; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
