package io.github.ysuestc.offerflow.application.dto;

import io.github.ysuestc.offerflow.application.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record StageRequest(@NotNull ApplicationStage stage, LocalDate stageOn, EndReason endReason,
        @Size(max = 16000) String remark, @NotNull @Min(0) Integer version) { }
