package io.github.ysuestc.offerflow.interview.controller;

import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.interview.dto.*;
import io.github.ysuestc.offerflow.interview.entity.*;
import io.github.ysuestc.offerflow.interview.service.InterviewService;
import io.github.ysuestc.offerflow.interview.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mysql")
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService service;
    @GetMapping
    public ApiResponse<PageResponse<InterviewSummary>> list(
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(required = false) @Positive Long applicationId,
            @RequestParam(required = false) InterviewFormat format,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(q, applicationId, format, page, size));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InterviewView> create(@Valid @RequestBody InterviewCreateRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @GetMapping("/{id}")
    public ApiResponse<InterviewView> get(@PathVariable @Positive long id) { return ApiResponse.success(service.get(id)); }
    @PutMapping("/{id}")
    public ApiResponse<InterviewView> update(@PathVariable @Positive long id, @Valid @RequestBody InterviewUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive long id, @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return ApiResponse.success(null);
    }
}
