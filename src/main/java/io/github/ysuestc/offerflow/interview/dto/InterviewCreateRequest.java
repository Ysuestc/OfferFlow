package io.github.ysuestc.offerflow.interview.dto;

import io.github.ysuestc.offerflow.interview.entity.InterviewFormat;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record InterviewCreateRequest(@NotNull @Positive Long applicationId, @NotBlank @Size(max = 64) String roundName, Instant interviewAt, InterviewFormat format,
        @Size(max = 40000) String questions, @Size(max = 40000) String answers,
        @Size(max = 40000) String review, @Size(max = 16000) String result) {}
