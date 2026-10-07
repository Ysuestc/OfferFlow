package io.github.ysuestc.offerflow.interview.vo;

import io.github.ysuestc.offerflow.interview.entity.InterviewFormat;
import java.time.Instant;

public record InterviewView(String id, String applicationId, String companyName, String positionName,
        String roundName, Instant interviewAt, InterviewFormat format, String questions, String answers,
        String review, String result, int version, Instant updatedAt) {}
