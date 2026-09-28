package com.smartmall.tlai.web.ai;

import com.smartmall.tlai.ai.application.AiChatService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.web.ai.dto.AiChatRequest;
import com.smartmall.tlai.web.ai.dto.AiChatResponse;
import com.smartmall.tlai.web.ai.dto.AiChatMessageRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(AiChatResponse.from(service.chat(request.messages().stream()
                .map(AiChatMessageRequest::toDomain).toList())));
    }
}
