package io.github.ysuestc.offerflow.todo.dto;

import io.github.ysuestc.offerflow.todo.entity.TodoKind;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record TodoUpdateRequest(@Positive Long interviewId, @NotNull TodoKind kind, @NotBlank @Size(max = 255) String title,
        Instant dueAt, @Size(max = 16000) String notes,
        @NotNull @Min(0) Integer version) {}
