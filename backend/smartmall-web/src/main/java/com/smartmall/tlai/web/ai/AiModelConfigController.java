package com.smartmall.tlai.web.ai;

import com.smartmall.tlai.ai.application.AiModelCommand;
import com.smartmall.tlai.ai.application.AiModelConfigService;
import com.smartmall.tlai.ai.model.AiProvider;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.ai.dto.AiModelConfigRequest;
import com.smartmall.tlai.web.ai.dto.AiModelConfigResponse;
import com.smartmall.tlai.web.ai.dto.AiModelEnabledRequest;
import com.smartmall.tlai.web.ai.dto.AiModelTestResponse;
import com.smartmall.tlai.web.ai.dto.AiProviderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Arrays;

@Validated
@RestController
@RequestMapping("/api/admin/ai/models")
@PreAuthorize("hasAuthority('system:model_manage')")
public class AiModelConfigController {
    private final AiModelConfigService service;

    public AiModelConfigController(AiModelConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AiModelConfigResponse>> list() {
        return ApiResponse.success(service.listSystem().stream()
                .map(AiModelConfigResponse::from).toList());
    }

    @GetMapping("/providers")
    public ApiResponse<List<AiProviderResponse>> providers() {
        return ApiResponse.success(Arrays.stream(AiProvider.values()).map(AiProviderResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiModelConfigResponse>> create(
            @Valid @RequestBody AiModelConfigRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = toCommand(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AiModelConfigResponse.from(
                        service.createSystem(command))));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiModelConfigResponse> update(
            @PathVariable @Min(1) long id, @Valid @RequestBody AiModelConfigRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(AiModelConfigResponse.from(
                service.updateSystem(id, toCommand(request, principal.id()))));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AiModelConfigResponse> activate(
            @PathVariable @Min(1) long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(AiModelConfigResponse.from(
                service.activateSystem(id, principal.id())));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<AiModelConfigResponse> changeEnabled(
            @PathVariable @Min(1) long id, @Valid @RequestBody AiModelEnabledRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(AiModelConfigResponse.from(
                service.changeSystemEnabled(id, request.enabled(), principal.id())));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<AiModelTestResponse> test(@PathVariable @Min(1) long id) {
        return ApiResponse.success(new AiModelTestResponse(true, service.testSystem(id)));
    }

    private static AiModelCommand toCommand(AiModelConfigRequest request, long operatorId) {
        return new AiModelCommand(request.name(), request.modelType(), request.provider(), request.baseUrl(),
                request.modelName(), request.apiKey(), request.temperature(), request.maxTokens(),
                request.embeddingDimensions(), operatorId);
    }
}
