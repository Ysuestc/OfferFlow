package io.github.ysuestc.offerflow.company.dto;

import io.github.ysuestc.offerflow.company.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank(message = "公司名称不能为空") @Size(max = 255) String name,
        @NotNull(message = "请选择公司类型") CompanyType type,
        @Size(max = 2048) String website,
        @Size(max = 16000) String notes) { }
