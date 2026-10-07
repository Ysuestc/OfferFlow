package io.github.ysuestc.offerflow.application.controller;

import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.application.dto.*;
import io.github.ysuestc.offerflow.application.entity.ApplicationStage;
import io.github.ysuestc.offerflow.application.service.ApplicationService;
import io.github.ysuestc.offerflow.application.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mysql")
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService service;
    @GetMapping
    public ApiResponse<PageResponse<ApplicationView>> list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(required = false) ApplicationStage stage,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(q, stage, page, size));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationView> create(@Valid @RequestBody ApplicationRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @GetMapping("/{id}")
    public ApiResponse<ApplicationView> get(@PathVariable @Positive long id) { return ApiResponse.success(service.get(id)); }
    @PutMapping("/{id}")
    public ApiResponse<ApplicationView> update(@PathVariable @Positive long id,
            @Valid @RequestBody ApplicationUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
    @PostMapping("/{id}/stages")
    public ApiResponse<ApplicationView> stage(@PathVariable @Positive long id, @Valid @RequestBody StageRequest request) {
        return ApiResponse.success(service.changeStage(id, request));
    }
    @GetMapping("/{id}/history")
    public ApiResponse<List<HistoryView>> history(@PathVariable @Positive long id) {
        return ApiResponse.success(service.history(id));
    }
}
