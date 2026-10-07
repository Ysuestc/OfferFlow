package io.github.ysuestc.offerflow.todo.vo;

import io.github.ysuestc.offerflow.todo.entity.TodoKind;
import java.time.Instant;

public record TodoView(String id, String applicationId, String companyName, String positionName,
        String interviewId, String interviewRound, TodoKind kind, String title, Instant dueAt,
        boolean completed, Instant completedAt, String notes, int version, Instant updatedAt) {}
