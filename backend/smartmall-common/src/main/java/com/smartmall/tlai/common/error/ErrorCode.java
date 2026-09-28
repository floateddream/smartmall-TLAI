package com.smartmall.tlai.common.error;

import org.springframework.http.HttpStatus;

/** 业务错误码与 HTTP 状态码的统一映射。 */
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "请求不合法"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请求参数校验失败"),
    USERNAME_EXISTS(HttpStatus.CONFLICT, "USERNAME_EXISTS", "用户名已存在"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "EMAIL_EXISTS", "邮箱已存在"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名、邮箱或密码错误"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权执行此操作"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "角色不存在"),
    ROLE_CODE_EXISTS(HttpStatus.CONFLICT, "ROLE_CODE_EXISTS", "角色编码已存在"),
    ROLE_ASSIGNMENT_REQUIRED(HttpStatus.BAD_REQUEST, "ROLE_ASSIGNMENT_REQUIRED", "用户至少需要一个有效角色"),
    SELF_PERMISSION_CHANGE_FORBIDDEN(HttpStatus.CONFLICT, "SELF_PERMISSION_CHANGE_FORBIDDEN", "不能修改当前登录账号自身的角色或状态"),
    LAST_ADMIN_REQUIRED(HttpStatus.CONFLICT, "LAST_ADMIN_REQUIRED", "系统必须保留至少一个启用的管理员"),
    BUILT_IN_ROLE_PROTECTED(HttpStatus.CONFLICT, "BUILT_IN_ROLE_PROTECTED", "该内置角色的关键配置受保护"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在"),
    PRODUCT_SKU_EXISTS(HttpStatus.CONFLICT, "PRODUCT_SKU_EXISTS", "商品 SKU 已存在"),
    PRODUCT_INACTIVE(HttpStatus.CONFLICT, "PRODUCT_INACTIVE", "商品已停用，不能创建订单"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "商品库存不足"),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "客户不存在"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"),
    ORDER_STATUS_INVALID(HttpStatus.CONFLICT, "ORDER_STATUS_INVALID", "当前订单状态不允许执行此操作"),
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "退款申请不存在"),
    REFUND_ALREADY_EXISTS(HttpStatus.CONFLICT, "REFUND_ALREADY_EXISTS", "该订单已有待审核的退款申请"),
    REFUND_STATUS_INVALID(HttpStatus.CONFLICT, "REFUND_STATUS_INVALID", "当前退款状态不允许执行此操作"),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "售后工单不存在"),
    TICKET_STATUS_INVALID(HttpStatus.CONFLICT, "TICKET_STATUS_INVALID", "工单状态只能按处理流程向前推进"),
    TICKET_CLOSED(HttpStatus.CONFLICT, "TICKET_CLOSED", "已关闭的工单不能继续回复"),
    AI_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_MODEL_NOT_FOUND", "模型配置不存在"),
    AI_MODEL_NAME_EXISTS(HttpStatus.CONFLICT, "AI_MODEL_NAME_EXISTS", "模型配置名称已存在"),
    AI_MODEL_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "AI_MODEL_NOT_CONFIGURED", "尚未启用可用的 AI 模型"),
    AI_CREDENTIAL_MISSING(HttpStatus.SERVICE_UNAVAILABLE, "AI_CREDENTIAL_MISSING", "尚未配置 AI 服务密钥"),
    AI_ENCRYPTION_KEY_MISSING(HttpStatus.SERVICE_UNAVAILABLE, "AI_ENCRYPTION_KEY_MISSING", "尚未配置 AI 密钥加密主密钥"),
    AI_ENCRYPTION_KEY_INVALID(HttpStatus.SERVICE_UNAVAILABLE, "AI_ENCRYPTION_KEY_INVALID", "AI 密钥加密主密钥格式无效"),
    AI_CREDENTIAL_DECRYPTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI_CREDENTIAL_DECRYPTION_FAILED", "AI 服务密钥无法解密，请重新配置"),
    AI_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", "AI 服务调用失败"),
    AI_EMBEDDING_DIMENSION_MISMATCH(HttpStatus.BAD_GATEWAY, "AI_EMBEDDING_DIMENSION_MISMATCH", "Embedding 模型返回的向量维度不匹配"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统内部错误");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String defaultMessage() { return defaultMessage; }
}
