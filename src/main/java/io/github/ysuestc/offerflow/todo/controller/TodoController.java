package io.github.ysuestc.offerflow.todo.controller;

import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.todo.dto.*;
import io.github.ysuestc.offerflow.todo.entity.*;
import io.github.ysuestc.offerflow.todo.service.TodoService;
import io.github.ysuestc.offerflow.todo.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mysql")
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
public class TodoController {
    private final TodoService service;
    @GetMapping
    public ApiResponse<PageResponse<TodoView>> list(
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(required = false) @Positive Long applicationId,
            @RequestParam(required = false) TodoKind kind,
            @RequestParam(required = false) Boolean completed, @RequestParam(required = false) TodoTiming timing,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(q, applicationId, kind, completed, timing, page, size));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TodoView> create(@Valid @RequestBody TodoCreateRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @GetMapping("/{id}")
    public ApiResponse<TodoView> get(@PathVariable @Positive long id) { return ApiResponse.success(service.get(id)); }
    @PutMapping("/{id}")
    public ApiResponse<TodoView> update(@PathVariable @Positive long id, @Valid @RequestBody TodoUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive long id, @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return ApiResponse.success(null);
    }
    @PutMapping("/{id}/completion")
    public ApiResponse<TodoView> complete(@PathVariable @Positive long id, @Valid @RequestBody CompletionRequest request) {
        return ApiResponse.success(service.complete(id, request));
    }
}
