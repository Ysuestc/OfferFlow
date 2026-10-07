package io.github.ysuestc.offerflow.application.vo;

import io.github.ysuestc.offerflow.application.entity.*;
import java.time.Instant;
import java.time.LocalDate;

public record HistoryView(String id, ApplicationStage stage, LocalDate stageOn, EndReason endReason,
        String remark, Instant recordedAt) {
    public static HistoryView from(ApplicationStageHistory entity) {
        return new HistoryView(entity.getId().toString(), entity.getStage(), entity.getStageOn(),
                entity.getEndReason(), entity.getRemark(), entity.getCreatedAt());
    }
}
