package com.smartmall.tlai.web.ticket;

import com.smartmall.tlai.business.ticket.application.AddTicketMessageCommand;
import com.smartmall.tlai.business.ticket.application.CreateTicketCommand;
import com.smartmall.tlai.business.ticket.application.TicketService;
import com.smartmall.tlai.business.ticket.application.UpdateTicketStatusCommand;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.web.ticket.dto.AddTicketMessageRequest;
import com.smartmall.tlai.web.ticket.dto.CreateTicketRequest;
import com.smartmall.tlai.web.ticket.dto.TicketResponse;
import com.smartmall.tlai.web.ticket.dto.UpdateTicketStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ticket:view')")
    public ApiResponse<PageResponse<TicketResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        var result = service.list(page, size, keyword, status, category);
        return ApiResponse.success(new PageResponse<>(result.items().stream()
                .map(TicketResponse::from).toList(), result.total(), result.page(), result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ticket:view')")
    public ApiResponse<TicketResponse> get(@PathVariable @Min(1) long id) {
        return ApiResponse.success(TicketResponse.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ticket:create')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new CreateTicketCommand(request.orderId(), request.category(), request.priority(),
                request.subject(), request.description(), principal.id(), principal.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TicketResponse.from(service.create(command))));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('ticket:reply')")
    public ApiResponse<TicketResponse> addMessage(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody AddTicketMessageRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new AddTicketMessageCommand(principal.id(), principal.username(), request.content());
        return ApiResponse.success(TicketResponse.from(service.addMessage(id, command)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ticket:status')")
    public ApiResponse<TicketResponse> updateStatus(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        var command = new UpdateTicketStatusCommand(request.status(), principal.id(), principal.username(),
                request.remark());
        return ApiResponse.success(TicketResponse.from(service.updateStatus(id, command)));
    }
}
