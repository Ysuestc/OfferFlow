package io.github.ysuestc.offerflow.company.vo;

import io.github.ysuestc.offerflow.company.entity.Company;
import io.github.ysuestc.offerflow.company.entity.CompanyType;

public record CompanyView(String id, String name, CompanyType type, String website, String notes) {
    public static CompanyView from(Company entity) {
        return new CompanyView(entity.getId().toString(), entity.getName(), entity.getType(),
                entity.getWebsite(), entity.getNotes());
    }
}
