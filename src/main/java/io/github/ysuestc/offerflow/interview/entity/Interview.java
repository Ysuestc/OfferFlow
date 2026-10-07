package io.github.ysuestc.offerflow.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("interview")
public class Interview extends BaseEntity {

    private Long applicationId;

    private String roundName;

    private Instant interviewAt;

    private InterviewFormat format;

    private String questions;

    private String answers;

    private String review;

    private String result;

    @Version
    private Integer version;
}
