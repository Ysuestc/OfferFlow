package io.github.ysuestc.offerflow.todo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.application.mapper.ApplicationMapper;
import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.interview.mapper.InterviewMapper;
import io.github.ysuestc.offerflow.todo.dto.*;
import io.github.ysuestc.offerflow.todo.entity.*;
import io.github.ysuestc.offerflow.todo.mapper.TodoMapper;
import io.github.ysuestc.offerflow.todo.vo.TodoView;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class TodoService {
    private final TodoMapper mapper;
    private final ApplicationMapper applications;
    private final InterviewMapper interviews;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<TodoView> list(String q, Long applicationId, TodoKind kind, Boolean completed,
            TodoTiming timing, int page, int size) {
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var until = now.plus(Duration.ofDays(7));
        String search = InputText.search(q);
        return new PageResponse<>(mapper.listViews(search, applicationId, kind, completed, timing, now, until,
                size, (long) (page - 1) * size),
                mapper.countViews(search, applicationId, kind, completed, timing, now, until), page, size);
    }

    public TodoView get(long id) {
        TodoView view = mapper.findView(id);
        if (view == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "待办不存在");
        return view;
    }

    private void validateInterview(long applicationId, Long interviewId) {
        if (interviewId == null) return;
        var interview = interviews.selectById(interviewId);
        if (interview == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "面试记录不存在");
        if (interview.getApplicationId() != applicationId)
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "关联面试必须属于同一投递档案");
    }

    @Transactional
    public TodoView create(TodoCreateRequest request) {
        if (applications.selectById(request.applicationId()) == null)
            throw new BusinessException(ApiErrorCode.NOT_FOUND, "投递档案不存在");
        validateInterview(request.applicationId(), request.interviewId());
        Todo entity = new Todo();
        entity.setApplicationId(request.applicationId());
        entity.setInterviewId(request.interviewId());
        entity.setKind(request.kind());
        entity.setTitle(request.title().strip());
        entity.setDueAt(request.dueAt());
        entity.setNotes(InputText.optional(request.notes()));
        entity.setCompleted(false);
        entity.setVersion(0);
        mapper.insert(entity);
        return get(entity.getId());
    }

    @Transactional
    public TodoView update(long id, TodoUpdateRequest request) {
        TodoView current = get(id);
        validateInterview(Long.parseLong(current.applicationId()), request.interviewId());
        int updated = mapper.update(null, Wrappers.<Todo>lambdaUpdate().eq(Todo::getId, id)
                .eq(Todo::getVersion, request.version())
                .set(Todo::getInterviewId, request.interviewId()).set(Todo::getKind, request.kind())
                .set(Todo::getTitle, request.title().strip()).set(Todo::getDueAt, request.dueAt())
                .set(Todo::getNotes, InputText.optional(request.notes()))
                .set(Todo::getVersion, request.version() + 1));
        if (updated == 0) conflict();
        return get(id);
    }

    @Transactional
    public TodoView complete(long id, CompletionRequest request) {
        // Row lock makes repeated completion a true no-op even with a concurrent writer.
        Todo entity = mapper.selectOne(Wrappers.<Todo>lambdaQuery().eq(Todo::getId, id).last("FOR UPDATE"));
        if (entity == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "待办不存在");
        if (!entity.getVersion().equals(request.version())) conflict();
        if (entity.getCompleted().equals(request.completed())) return get(id);
        mapper.update(null, Wrappers.<Todo>lambdaUpdate().eq(Todo::getId, id)
                .eq(Todo::getVersion, request.version()).set(Todo::getCompleted, request.completed())
                .set(Todo::getCompletedAt, request.completed() ? clock.instant().truncatedTo(ChronoUnit.MICROS) : null)
                .set(Todo::getVersion, request.version() + 1));
        return get(id);
    }

    @Transactional
    public void delete(long id, int version) {
        get(id);
        if (mapper.delete(Wrappers.<Todo>lambdaQuery().eq(Todo::getId, id)
                .eq(Todo::getVersion, version)) == 0) conflict();
    }

    private void conflict() {
        throw new BusinessException(ApiErrorCode.CONFLICT, "待办已更新，请重新加载后操作");
    }
}
