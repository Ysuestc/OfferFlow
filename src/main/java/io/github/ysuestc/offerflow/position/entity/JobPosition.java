package io.github.ysuestc.offerflow.position.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job_position")
public class JobPosition extends BaseEntity {

    private Long companyId;

    private String name;

    private String location;

    private String direction;

    private String jd;

    private String recruitmentBatch;
}
