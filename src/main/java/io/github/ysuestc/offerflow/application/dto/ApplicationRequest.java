package io.github.ysuestc.offerflow.application.dto;

import io.github.ysuestc.offerflow.application.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ApplicationRequest(@NotNull @Positive Long jobPositionId,
        @Size(max = 64) String channel, LocalDate appliedOn,
        @NotNull ApplicationStage stage, LocalDate stageOn, EndReason endReason, Boolean submitted,
        @Size(max = 16000) String notes) { }
