package com.smartmall.tlai.web.ai;

import com.smartmall.tlai.agent.application.AgentConversationService;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.ai.dto.AiConversationMessageRequest;
import com.smartmall.tlai.web.ai.dto.AiConversationMessageResponse;
import com.smartmall.tlai.web.ai.dto.AiConversationResponse;
import com.smartmall.tlai.web.ai.dto.AiConversationTurnResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/ai/conversations")
public class AiConversationController {
    private static final Logger log = LoggerFactory.getLogger(AiConversationController.class);
    private static final long SSE_TIMEOUT_MS = 135_000L;
    private static final int STREAM_CHUNK_SIZE = 18;

    private final AgentConversationService service;
    private final TaskExecutor taskExecutor;

    public AiConversationController(AgentConversationService service,
                                    @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.service = service;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiConversationResponse> create(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(AiConversationResponse.from(service.create(userContext(principal))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<PageResponse<AiConversationResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var result = service.list(userContext(principal), page, size);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(AiConversationResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<java.util.List<AiConversationMessageResponse>> messages(
            @PathVariable @Min(1) long id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(service.messages(id, userContext(principal)).stream()
                .map(AiConversationMessageResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<Void> archive(@PathVariable @Min(1) long id,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        service.archive(id, userContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('ai:chat')")
    public SseEmitter stream(@PathVariable @Min(1) long id,
                             @Valid @RequestBody AiConversationMessageRequest request,
                             @AuthenticationPrincipal AppUserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AgentUserContext user = userContext(principal);
        taskExecutor.execute(() -> processStream(emitter, id, request.content(), user));
        return emitter;
    }

    private void processStream(SseEmitter emitter, long conversationId, String content,
                               AgentUserContext user) {
        try {
            send(emitter, "accepted", Map.of("conversationId", conversationId));
            AiConversationTurnResponse turn = AiConversationTurnResponse.from(
                    service.reply(conversationId, content, user));
            streamContent(emitter, turn.reply().content());
            send(emitter, "result", turn);
            send(emitter, "done", Map.of("conversationId", conversationId));
            emitter.complete();
        } catch (IOException exception) {
            // 容器会处理已经断开的连接，此处不能再次完成流，否则会触发重复的异步异常派发。
            log.debug("AI 对话流连接已断开：会话ID={}，用户ID={}",
                    conversationId, user.userId());
        } catch (BusinessException exception) {
            log.warn("AI 对话流处理失败：会话ID={}，用户ID={}，错误码={}",
                    conversationId, user.userId(), exception.getErrorCode().code());
            safeError(emitter, exception.getErrorCode().code(), exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("AI 对话流发生未预期异常：会话ID={}，用户ID={}",
                    conversationId, user.userId(), exception);
            safeError(emitter, ErrorCode.INTERNAL_ERROR.code(), "AI 请求处理失败，请稍后重试");
        }
    }

    private void streamContent(SseEmitter emitter, String content) throws IOException {
        for (int start = 0; start < content.length();) {
            int end = Math.min(start + STREAM_CHUNK_SIZE, content.length());
            if (end < content.length() && Character.isHighSurrogate(content.charAt(end - 1))) end--;
            send(emitter, "delta", Map.of("content", content.substring(start, end)));
            start = end;
        }
    }

    private static void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private static void safeError(SseEmitter emitter, String code, String message) {
        try {
            send(emitter, "error", Map.of("code", code, "message", message));
            emitter.complete();
        } catch (IOException exception) {
            // 客户端已经离开，继续完成或发送错误只会再次写入失效的响应。
            log.debug("SSE 客户端已断开，跳过发送 AI 对话错误事件");
        }
    }

    private static AgentUserContext userContext(AppUserPrincipal principal) {
        return new AgentUserContext(principal.id(), principal.username(), Set.copyOf(principal.permissions()));
    }
}
