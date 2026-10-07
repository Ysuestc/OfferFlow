package io.github.ysuestc.offerflow.application.vo;

import io.github.ysuestc.offerflow.application.entity.*;
import java.time.Instant;
import java.time.LocalDate;

public record ApplicationView(String id, String jobPositionId, String companyId, String companyName,
        String positionName, String location, String direction, String recruitmentBatch, String channel,
        LocalDate appliedOn, boolean submitted, ApplicationStage currentStage, LocalDate currentStageOn,
        EndReason endReason, String notes, int version, Instant updatedAt) { }
