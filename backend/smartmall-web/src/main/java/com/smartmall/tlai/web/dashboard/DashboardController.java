package com.smartmall.tlai.web.dashboard;

import com.smartmall.tlai.business.dashboard.application.DashboardService;
import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.web.dashboard.dto.DashboardOverviewResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.success(DashboardOverviewResponse.from(service.overview()));
    }
}
