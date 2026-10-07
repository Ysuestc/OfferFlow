package io.github.ysuestc.offerflow.company.controller;

import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.company.dto.CompanyRequest;
import io.github.ysuestc.offerflow.company.service.CompanyService;
import io.github.ysuestc.offerflow.company.vo.CompanyView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mysql")
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService service;

    @GetMapping
    public ApiResponse<PageResponse<CompanyView>> list(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(q, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyView> create(@Valid @RequestBody CompanyRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyView> get(@PathVariable @Positive long id) { return ApiResponse.success(service.get(id)); }

    @PutMapping("/{id}")
    public ApiResponse<CompanyView> update(@PathVariable @Positive long id, @Valid @RequestBody CompanyRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
