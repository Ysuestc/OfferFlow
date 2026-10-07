package io.github.ysuestc.offerflow.position.controller;

import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.position.dto.PositionRequest;
import io.github.ysuestc.offerflow.position.service.PositionService;
import io.github.ysuestc.offerflow.position.vo.PositionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mysql")
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService service;
    @GetMapping
    public ApiResponse<PageResponse<PositionView>> list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(required = false) @Positive Long companyId,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(q, companyId, page, size));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PositionView> create(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @GetMapping("/{id}")
    public ApiResponse<PositionView> get(@PathVariable @Positive long id) { return ApiResponse.success(service.get(id)); }
    @PutMapping("/{id}")
    public ApiResponse<PositionView> update(@PathVariable @Positive long id, @Valid @RequestBody PositionRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
