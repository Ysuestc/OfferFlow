package io.github.ysuestc.offerflow.health.controller;

import io.github.ysuestc.offerflow.common.api.ApiResponse;
import io.github.ysuestc.offerflow.health.vo.HealthResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("UP"));
    }
}
