package com.smartmall.tlai.web.ai;

import com.smartmall.tlai.agent.application.ReadOnlyAgentService;
import com.smartmall.tlai.agent.application.PendingAgentActionService;
import com.smartmall.tlai.agent.application.AgentCapabilityRegistry;
import com.smartmall.tlai.agent.model.AgentUserContext;
import com.smartmall.tlai.ai.application.AiChatService;
import com.smartmall.tlai.ai.model.AiCallContext;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.rag.application.RagChatService;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.ai.dto.AiChatRequest;
import com.smartmall.tlai.web.ai.dto.AiChatResponse;
import com.smartmall.tlai.web.ai.dto.AiChatMessageRequest;
import com.smartmall.tlai.web.ai.dto.AiAgentActionRequest;
import com.smartmall.tlai.web.ai.dto.AiAgentActionResponse;
import com.smartmall.tlai.web.ai.dto.AiCapabilityResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService service;
    private final RagChatService ragService;
    private final ReadOnlyAgentService agentService;
    private final PendingAgentActionService actionService;
    private final AgentCapabilityRegistry capabilityRegistry;

    public AiChatController(AiChatService service, RagChatService ragService,
                            ReadOnlyAgentService agentService, PendingAgentActionService actionService,
                            AgentCapabilityRegistry capabilityRegistry) {
        this.service = service;
        this.ragService = ragService;
        this.agentService = agentService;
        this.actionService = actionService;
        this.capabilityRegistry = capabilityRegistry;
    }

    @GetMapping("/capabilities")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<List<AiCapabilityResponse>> capabilities(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(capabilityRegistry.availableTo(userContext(principal)).stream()
                .map(AiCapabilityResponse::from).toList());
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:chat') and (!#request.knowledgeEnabled or hasAuthority('knowledge:view'))")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request,
                                            @AuthenticationPrincipal AppUserPrincipal principal) {
        var messages = request.messages().stream().map(AiChatMessageRequest::toDomain).toList();
        AgentUserContext user = userContext(principal);
        if (request.agentEnabled()) {
            return ApiResponse.success(AiChatResponse.from(agentService.chat(messages, user)));
        }
        AiCallContext callContext = new AiCallContext(principal.id(), null, null);
        return ApiResponse.success(request.knowledgeEnabled()
                ? AiChatResponse.from(ragService.chat(messages, callContext))
                : AiChatResponse.from(service.chat(messages, callContext)));
    }

    @PostMapping("/agent/confirm")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiAgentActionResponse> confirmAction(
            @Valid @RequestBody AiAgentActionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(AiAgentActionResponse.from(
                actionService.confirm(request.token(), userContext(principal))));
    }

    @PostMapping("/agent/cancel")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiAgentActionResponse> cancelAction(
            @Valid @RequestBody AiAgentActionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(AiAgentActionResponse.from(
                actionService.cancel(request.token(), userContext(principal))));
    }

    private AgentUserContext userContext(AppUserPrincipal principal) {
        return new AgentUserContext(principal.id(), principal.username(),
                Set.copyOf(principal.permissions()));
    }
}
