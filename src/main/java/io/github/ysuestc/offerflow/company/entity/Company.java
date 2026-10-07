package io.github.ysuestc.offerflow.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("company")
public class Company extends BaseEntity {

    private String name;

    private CompanyType type;

    private String website;

    private String notes;
}
