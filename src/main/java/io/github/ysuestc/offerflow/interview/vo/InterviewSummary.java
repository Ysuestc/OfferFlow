package io.github.ysuestc.offerflow.interview.vo;

import io.github.ysuestc.offerflow.interview.entity.InterviewFormat;
import java.time.Instant;

public record InterviewSummary(String id, String applicationId, String companyName, String positionName,
        String roundName, Instant interviewAt, InterviewFormat format, int version, Instant updatedAt) {}
