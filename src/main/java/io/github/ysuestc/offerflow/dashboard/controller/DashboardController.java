package io.github.ysuestc.offerflow.dashboard.controller;

import io.github.ysuestc.offerflow.common.api.ApiResponse;
import io.github.ysuestc.offerflow.dashboard.service.DashboardService;
import io.github.ysuestc.offerflow.dashboard.vo.DashboardView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mysql")
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;
    @GetMapping
    public ApiResponse<DashboardView> get() { return ApiResponse.success(service.get()); }
}
