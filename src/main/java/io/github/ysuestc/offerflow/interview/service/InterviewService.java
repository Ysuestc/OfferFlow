package io.github.ysuestc.offerflow.interview.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.application.mapper.ApplicationMapper;
import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.interview.dto.*;
import io.github.ysuestc.offerflow.interview.entity.*;
import io.github.ysuestc.offerflow.interview.mapper.InterviewMapper;
import io.github.ysuestc.offerflow.interview.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class InterviewService {
    private final InterviewMapper mapper;
    private final ApplicationMapper applications;

    @Transactional(readOnly = true)
    public PageResponse<InterviewSummary> list(String q, Long applicationId, InterviewFormat format, int page, int size) {
        String search = InputText.search(q);
        return new PageResponse<>(mapper.listViews(search, applicationId, format, size, (long) (page - 1) * size),
                mapper.countViews(search, applicationId, format), page, size);
    }

    public InterviewView get(long id) {
        InterviewView view = mapper.findView(id);
        if (view == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "面试记录不存在");
        return view;
    }

    @Transactional
    public InterviewView create(InterviewCreateRequest request) {
        if (applications.selectById(request.applicationId()) == null)
            throw new BusinessException(ApiErrorCode.NOT_FOUND, "投递档案不存在");
        Interview entity = new Interview();
        entity.setApplicationId(request.applicationId());
        entity.setRoundName(request.roundName().strip());
        entity.setInterviewAt(request.interviewAt());
        entity.setFormat(request.format());
        entity.setQuestions(InputText.optional(request.questions()));
        entity.setAnswers(InputText.optional(request.answers()));
        entity.setReview(InputText.optional(request.review()));
        entity.setResult(InputText.optional(request.result()));
        entity.setVersion(0);
        mapper.insert(entity);
        return get(entity.getId());
    }

    @Transactional
    public InterviewView update(long id, InterviewUpdateRequest request) {
        get(id);
        // Explicit SET null allows clearing unknown dates and optional review fields.
        int updated = mapper.update(null, Wrappers.<Interview>lambdaUpdate()
                .eq(Interview::getId, id).eq(Interview::getVersion, request.version())
                .set(Interview::getRoundName, request.roundName().strip())
                .set(Interview::getInterviewAt, request.interviewAt()).set(Interview::getFormat, request.format())
                .set(Interview::getQuestions, InputText.optional(request.questions()))
                .set(Interview::getAnswers, InputText.optional(request.answers()))
                .set(Interview::getReview, InputText.optional(request.review()))
                .set(Interview::getResult, InputText.optional(request.result()))
                .set(Interview::getVersion, request.version() + 1));
        if (updated == 0) conflict();
        return get(id);
    }

    @Transactional
    public void delete(long id, int version) {
        get(id);
        if (mapper.delete(Wrappers.<Interview>lambdaQuery().eq(Interview::getId, id)
                .eq(Interview::getVersion, version)) == 0) conflict();
    }

    private void conflict() {
        throw new BusinessException(ApiErrorCode.CONFLICT, "面试记录已更新，请重新加载后操作");
    }
}
