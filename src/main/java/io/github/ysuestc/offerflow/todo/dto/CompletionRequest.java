package io.github.ysuestc.offerflow.todo.dto;

import jakarta.validation.constraints.*;

public record CompletionRequest(@NotNull Boolean completed, @NotNull @Min(0) Integer version) {}
