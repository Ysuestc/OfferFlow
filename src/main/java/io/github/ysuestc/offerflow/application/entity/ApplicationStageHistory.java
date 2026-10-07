package io.github.ysuestc.offerflow.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("application_stage_history")
public class ApplicationStageHistory extends BaseEntity {

    private Long applicationId;

    private ApplicationStage stage;

    private LocalDate stageOn;

    private EndReason endReason;

    private String remark;
}
