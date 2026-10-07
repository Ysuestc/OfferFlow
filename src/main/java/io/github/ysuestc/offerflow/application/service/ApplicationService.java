package io.github.ysuestc.offerflow.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.application.dto.*;
import io.github.ysuestc.offerflow.application.entity.*;
import io.github.ysuestc.offerflow.application.mapper.*;
import io.github.ysuestc.offerflow.application.vo.*;
import io.github.ysuestc.offerflow.position.service.PositionService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationMapper mapper;
    private final ApplicationStageHistoryMapper histories;
    private final PositionService positions;

    public PageResponse<ApplicationView> list(String q, ApplicationStage stage, int page, int size) {
        String search = InputText.search(q);
        return new PageResponse<>(mapper.listViews(search, stage, size, (long) (page - 1) * size),
                mapper.countViews(search, stage), page, size);
    }

    public ApplicationView get(long id) {
        ApplicationView view = mapper.findView(id);
        if (view == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "投递档案不存在");
        return view;
    }

    public List<HistoryView> history(long id) {
        get(id);
        return histories.selectList(Wrappers.<ApplicationStageHistory>lambdaQuery()
                .eq(ApplicationStageHistory::getApplicationId, id).orderByAsc(ApplicationStageHistory::getId))
                .stream().map(HistoryView::from).toList();
    }

    @Transactional
    public ApplicationView create(ApplicationRequest request) {
        positions.get(request.jobPositionId());
        validateStage(request.stage(), request.endReason());
        Application entity = new Application();
        entity.setJobPositionId(request.jobPositionId());
        entity.setChannel(InputText.optional(request.channel()));
        entity.setSubmitted(submittedStage(request.stage())
                || request.stage() == ApplicationStage.ENDED && Boolean.TRUE.equals(request.submitted()));
        validateDate(entity.getSubmitted(), request.appliedOn());
        entity.setAppliedOn(request.appliedOn());
        entity.setCurrentStage(request.stage());
        entity.setCurrentStageOn(request.stageOn());
        entity.setEndReason(request.endReason());
        entity.setNotes(InputText.optional(request.notes()));
        entity.setVersion(0);
        mapper.insert(entity);
        append(entity, "建立投递档案");
        return get(entity.getId());
    }

    @Transactional
    public ApplicationView update(long id, ApplicationUpdateRequest request) {
        Application current = requireVersion(id, request.version());
        validateDate(current.getSubmitted(), request.appliedOn());
        // Explicit SET retains full-replacement semantics, including clearing unknown dates and notes.
        int rows = mapper.update(null, Wrappers.<Application>lambdaUpdate()
                .eq(Application::getId, id).eq(Application::getVersion, request.version())
                .set(Application::getChannel, InputText.optional(request.channel()))
                .set(Application::getAppliedOn, request.appliedOn())
                .set(Application::getNotes, InputText.optional(request.notes()))
                .set(Application::getVersion, request.version() + 1));
        requireUpdated(rows);
        return get(id);
    }

    @Transactional
    public ApplicationView changeStage(long id, StageRequest request) {
        Application current = requireVersion(id, request.version());
        validateStage(request.stage(), request.endReason());
        if (current.getCurrentStage() == request.stage()
                && Objects.equals(current.getCurrentStageOn(), request.stageOn())
                && current.getEndReason() == request.endReason()) return get(id);
        current.setCurrentStage(request.stage());
        current.setCurrentStageOn(request.stageOn());
        current.setEndReason(request.endReason());
        current.setSubmitted(current.getSubmitted() || submittedStage(request.stage()));
        requireUpdated(mapper.updateById(current));
        append(current, InputText.optional(request.remark()));
        return get(id);
    }

    private Application requireVersion(long id, int version) {
        Application entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "投递档案不存在");
        if (entity.getVersion() != version) throw conflict();
        return entity;
    }

    private void append(Application entity, String remark) {
        ApplicationStageHistory history = new ApplicationStageHistory();
        history.setApplicationId(entity.getId());
        history.setStage(entity.getCurrentStage());
        history.setStageOn(entity.getCurrentStageOn());
        history.setEndReason(entity.getEndReason());
        history.setRemark(remark);
        histories.insert(history);
    }

    private static boolean submittedStage(ApplicationStage stage) {
        return stage != ApplicationStage.TO_APPLY && stage != ApplicationStage.ENDED;
    }

    private static void validateStage(ApplicationStage stage, EndReason reason) {
        if ((stage == ApplicationStage.ENDED) != (reason != null)) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "已结束须选择结束原因，其他阶段不能填写结束原因");
        }
    }

    private static void validateDate(boolean submitted, java.time.LocalDate date) {
        if (!submitted && date != null) throw new BusinessException(ApiErrorCode.BAD_REQUEST, "待投递档案不能填写投递日期");
    }

    private static void requireUpdated(int rows) {
        if (rows != 1) throw conflict();
    }

    private static BusinessException conflict() {
        return new BusinessException(ApiErrorCode.CONFLICT, "档案已更新，请刷新后再操作");
    }
}
