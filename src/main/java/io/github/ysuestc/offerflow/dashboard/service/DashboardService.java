package io.github.ysuestc.offerflow.dashboard.service;

import io.github.ysuestc.offerflow.dashboard.mapper.DashboardMapper;
import io.github.ysuestc.offerflow.dashboard.vo.DashboardView;
import io.github.ysuestc.offerflow.interview.mapper.InterviewMapper;
import io.github.ysuestc.offerflow.todo.entity.TodoTiming;
import io.github.ysuestc.offerflow.todo.mapper.TodoMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardMapper mapper;
    private final InterviewMapper interviews;
    private final TodoMapper todos;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardView get() {
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var until = now.plus(Duration.ofDays(7));
        return new DashboardView(mapper.counts(), now, until,
                todos.countViews(null, null, null, false, TodoTiming.UPCOMING, now, until),
                todos.countViews(null, null, null, false, TodoTiming.OVERDUE, now, until),
                todos.countViews(null, null, null, false, TodoTiming.UNDATED, now, until),
                mapper.recentApplications(), interviews.recent(now),
                todos.listViews(null, null, null, false, TodoTiming.UPCOMING, now, until, 5, 0),
                todos.listViews(null, null, null, false, TodoTiming.OVERDUE, now, until, 5, 0));
    }
}
