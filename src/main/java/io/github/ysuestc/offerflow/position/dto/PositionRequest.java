package io.github.ysuestc.offerflow.position.dto;

import jakarta.validation.constraints.*;

public record PositionRequest(
        @NotNull @Positive Long companyId,
        @NotBlank(message = "岗位名称不能为空") @Size(max = 255) String name,
        @Size(max = 255) String location,
        @Size(max = 128) String direction,
        @Size(max = 40000) String jd,
        @Size(max = 64) String recruitmentBatch) { }
