package io.github.ysuestc.offerflow.dashboard.vo;

import io.github.ysuestc.offerflow.application.vo.ApplicationView;
import io.github.ysuestc.offerflow.interview.vo.InterviewSummary;
import io.github.ysuestc.offerflow.todo.vo.TodoView;
import java.time.Instant;
import java.util.List;

public record DashboardView(DashboardCounts counts, Instant generatedAt, Instant upcomingUntil,
        long upcomingTodoCount, long overdueTodoCount, long undatedTodoCount,
        List<ApplicationView> recentApplications, List<InterviewSummary> recentInterviews,
        List<TodoView> upcomingTodos, List<TodoView> overdueTodos) {}
