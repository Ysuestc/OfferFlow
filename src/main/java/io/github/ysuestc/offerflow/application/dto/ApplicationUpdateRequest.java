package io.github.ysuestc.offerflow.application.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ApplicationUpdateRequest(@Size(max = 64) String channel, LocalDate appliedOn,
        @Size(max = 16000) String notes, @NotNull @Min(0) Integer version) { }
