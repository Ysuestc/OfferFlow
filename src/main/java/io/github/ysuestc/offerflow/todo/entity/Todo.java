package io.github.ysuestc.offerflow.todo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("todo")
public class Todo extends BaseEntity {

    private Long applicationId;

    private Long interviewId;

    private TodoKind kind;

    private String title;

    private Instant dueAt;

    private Boolean completed;

    private Instant completedAt;

    private String notes;
}
