package io.github.ysuestc.offerflow.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("application")
public class Application extends BaseEntity {

    private Long jobPositionId;

    private String channel;

    private LocalDate appliedOn;

    private Boolean submitted;

    private ApplicationStage currentStage;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate currentStageOn;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private EndReason endReason;

    private String notes;

    @Version
    private Integer version;
}
