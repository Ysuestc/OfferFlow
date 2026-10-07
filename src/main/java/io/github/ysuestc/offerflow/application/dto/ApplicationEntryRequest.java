package io.github.ysuestc.offerflow.application.dto;

import io.github.ysuestc.offerflow.application.entity.*;
import io.github.ysuestc.offerflow.company.entity.CompanyType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ApplicationEntryRequest(@Positive Long companyId, @Size(max = 255) String companyName,
        CompanyType companyType, @NotBlank(message = "岗位名称不能为空") @Size(max = 255) String positionName,
        @Size(max = 255) String location, @Size(max = 128) String direction,
        @Size(max = 64) String recruitmentBatch, @Size(max = 40000) String jd,
        @Size(max = 64) String channel, LocalDate appliedOn,
        @NotNull ApplicationStage stage, LocalDate stageOn, EndReason endReason, Boolean submitted,
        @Size(max = 16000) String notes) {

    @AssertTrue(message = "请填写公司名称或选择已有公司，不能同时指定")
    public boolean isCompanySpecified() {
        boolean hasName = companyName != null && !companyName.isBlank();
        return (companyId != null) != hasName;
    }
}
