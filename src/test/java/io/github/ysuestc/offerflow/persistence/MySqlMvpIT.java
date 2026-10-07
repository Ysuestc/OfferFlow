package io.github.ysuestc.offerflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql")
@Import(MySqlMvpIT.FixedTime.class)
class MySqlMvpIT {
    static final Instant NOW = Instant.parse("2026-10-07T02:00:00Z");
    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        var database = MySqlTestSettings.mvp();
        database.requireEmptySchema();
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
    }

    @BeforeEach
    void emptyOwnedSchema() {
        for (String table : List.of("todo", "interview", "application_stage_history", "application", "job_position", "company"))
            jdbc.update("DELETE FROM " + table);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ONLINE", "OFFLINE", "AI"})
    void savesFullInterviewWithoutChangingApplication(String format) {
        String app = application("FIRST_INTERVIEW");
        var interview = request(HttpMethod.POST, "/interviews", Map.of("applicationId", app,
                "roundName", " 一面🌟 ", "interviewAt", "2026-10-07T10:00:00.123456+08:00", "format", format,
                "questions", "问题\n锁与事务", "answers", "我的回答", "review", "完整复盘", "result", "待反馈"), 201);
        String id = interview.path("id").asText();
        assertThat(interview.path("id").isTextual()).isTrue();
        assertThat(interview.path("roundName").asText()).isEqualTo("一面🌟");
        assertThat(interview.path("interviewAt").asText()).isEqualTo("2026-10-07T02:00:00.123456Z");
        assertThat(get("/interviews/" + id).path("review").asText()).isEqualTo("完整复盘");
        var page = get("/interviews?applicationId=" + app + "&format=" + format);
        assertThat(page.path("total").asLong()).isEqualTo(1);
        assertThat(page.path("items").get(0).has("review")).isFalse();
        assertThat(get("/applications/" + app).path("version").asInt()).isZero();
        assertThat(get("/applications/" + app + "/history").size()).isEqualTo(1);
    }

    @Test
    void clearsInterviewOptionalFieldsAndRejectsStaleChanges() {
        String app = application("SUBMITTED");
        String id = interview(app, "2026-10-06T02:00:00Z");
        var updated = request(HttpMethod.PUT, "/interviews/" + id,
                Map.of("roundName", "技术二面", "version", 0, "applicationId", "999999"), 200);
        assertThat(updated.path("applicationId").asText()).isEqualTo(app);
        for (String field : List.of("interviewAt", "format", "questions", "answers", "review", "result"))
            assertThat(updated.path(field).isNull()).as(field).isTrue();
        request(HttpMethod.PUT, "/interviews/" + id, Map.of("roundName", "旧窗口内容", "version", 0), 409);
        request(HttpMethod.DELETE, "/interviews/" + id + "?version=0", null, 409);
        assertThat(get("/interviews/" + id).path("roundName").asText()).isEqualTo("技术二面");
        request(HttpMethod.DELETE, "/interviews/" + id + "?version=1", null, 200);
        request(HttpMethod.GET, "/interviews/" + id, null, 404);
    }

    @Test
    void protectsReferencedInterviewUntilTodoIsUnlinked() {
        String app = application("SUBMITTED");
        String id = interview(app, null);
        String todo = request(HttpMethod.POST, "/todos", Map.of("applicationId", app,
                "interviewId", id, "kind", "INTERVIEW", "title", "准备面试"), 201).path("id").asText();
        request(HttpMethod.DELETE, "/interviews/" + id + "?version=0", null, 409);
        request(HttpMethod.PUT, "/todos/" + todo, Map.of("kind", "INTERVIEW", "title", "解除面试关联", "version", 0), 200);
        assertThat(get("/todos/" + todo).path("interviewId").isNull()).isTrue();
        request(HttpMethod.DELETE, "/interviews/" + id + "?version=0", null, 200);
        assertThat(get("/todos/" + todo).path("title").asText()).isEqualTo("解除面试关联");
    }

    @ParameterizedTest
    @ValueSource(strings = {"WRITTEN_TEST", "INTERVIEW", "ASSESSMENT", "MATERIAL", "OFFER_DEADLINE", "OTHER"})
    void supportsEveryTodoKindAndUnknownDeadlines(String kind) {
        String app = application("SUBMITTED");
        String id = todo(app, kind, null);
        assertThat(get("/todos/" + id).path("completed").asBoolean()).isFalse();
        assertThat(get("/todos/" + id).path("dueAt").isNull()).isTrue();
        assertThat(get("/todos?kind=" + kind + "&timing=UNDATED&applicationId=" + app).path("total").asLong()).isEqualTo(1);
        var updated = request(HttpMethod.PUT, "/todos/" + id, Map.of("title", "补充事项",
                "kind", kind, "dueAt", "2026-10-08T10:00:00+08:00", "version", 0), 200);
        assertThat(updated.path("dueAt").asText()).isEqualTo("2026-10-08T02:00:00Z");
        var cleared = request(HttpMethod.PUT, "/todos/" + id,
                Map.of("title", "时间再次未定", "kind", kind, "version", 1), 200);
        assertThat(cleared.path("dueAt").isNull()).isTrue();
        assertThat(cleared.path("notes").isNull()).isTrue();
    }

    @Test
    void rejectsMissingAndCrossApplicationAssociations() {
        String app = application("SUBMITTED");
        String foreignInterview = interview(application("SUBMITTED"), null);
        request(HttpMethod.POST, "/interviews", Map.of("applicationId", "999999", "roundName", "一面"), 404);
        request(HttpMethod.POST, "/todos", Map.of("applicationId", "999999", "kind", "OTHER", "title", "事项"), 404);
        request(HttpMethod.POST, "/todos", Map.of("applicationId", app, "interviewId", foreignInterview,
                "kind", "INTERVIEW", "title", "关联错误"), 400);
        request(HttpMethod.POST, "/todos", Map.of("applicationId", app, "interviewId", "999999",
                "kind", "INTERVIEW", "title", "缺失面试"), 404);
        String id = todo(app, "OTHER", null);
        request(HttpMethod.PUT, "/todos/" + id, Map.of("kind", "INTERVIEW", "title", "错误更新",
                "interviewId", foreignInterview, "version", 0), 400);
        assertThat(get("/todos/" + id).path("version").asInt()).isZero();
    }

    @Test
    void completesIdempotentlyEditsAndReopensWithServerTime() {
        String id = todo(application("SUBMITTED"), "MATERIAL", null);
        var completed = request(HttpMethod.PUT, "/todos/" + id + "/completion",
                Map.of("completed", true, "version", 0), 200);
        assertThat(completed.path("completedAt").asText()).isEqualTo(NOW.toString());
        var repeated = request(HttpMethod.PUT, "/todos/" + id + "/completion",
                Map.of("completed", true, "version", 1), 200);
        assertThat(repeated).isEqualTo(completed);
        request(HttpMethod.PUT, "/todos/" + id + "/completion", Map.of("completed", false, "version", 0), 409);
        var edited = request(HttpMethod.PUT, "/todos/" + id, Map.of("kind", "MATERIAL", "title", "完成后补充", "version", 1), 200);
        assertThat(edited.path("completedAt")).isEqualTo(completed.path("completedAt"));
        var opened = request(HttpMethod.PUT, "/todos/" + id + "/completion", Map.of("completed", false, "version", 2), 200);
        assertThat(opened.path("completed").asBoolean()).isFalse();
        assertThat(opened.path("completedAt").isNull()).isTrue();
        assertThat(opened.path("title").asText()).isEqualTo("完成后补充");
        request(HttpMethod.DELETE, "/todos/" + id + "?version=2", null, 409);
        request(HttpMethod.DELETE, "/todos/" + id + "?version=3", null, 200);
    }

    @Test
    void filtersDeadlinesAtExactSevenDayBoundaries() {
        String app = application("SUBMITTED");
        String overdue = todo(app, "OTHER", NOW.minusNanos(1000).toString());
        String today = todo(app, "OTHER", NOW.toString());
        String inside = todo(app, "OTHER", NOW.plus(Duration.ofDays(7)).minusNanos(1000).toString());
        todo(app, "OTHER", NOW.plus(Duration.ofDays(7)).toString());
        todo(app, "OTHER", null);
        String completed = todo(app, "OTHER", NOW.minusSeconds(1).toString());
        request(HttpMethod.PUT, "/todos/" + completed + "/completion", Map.of("completed", true, "version", 0), 200);
        var upcoming = get("/todos?timing=UPCOMING");
        assertThat(upcoming.path("total").asLong()).isEqualTo(2);
        assertThat(upcoming.path("items").get(0).path("id").asText()).isEqualTo(today);
        assertThat(upcoming.path("items").get(1).path("id").asText()).isEqualTo(inside);
        assertThat(get("/todos?timing=OVERDUE").path("items").get(0).path("id").asText()).isEqualTo(overdue);
        assertThat(get("/todos?timing=OVERDUE").path("total").asLong()).isEqualTo(1);
        assertThat(get("/todos?timing=UNDATED").path("total").asLong()).isEqualTo(1);
        assertThat(get("/todos?completed=true&timing=UPCOMING").path("total").asLong()).isZero();
    }

    @Test
    void countsHistoricalFactsAndCurrentStagesPrecisely() {
        application("TO_APPLY");
        application("SUBMITTED");
        application("ASSESSMENT");
        for (String stage : List.of("FIRST_INTERVIEW", "SECOND_INTERVIEW", "THIRD_INTERVIEW", "HR_INTERVIEW"))
            application(stage);
        application("OFFER");
        application("REJECTED");
        String ended = application("OFFER");
        request(HttpMethod.POST, "/applications/" + ended + "/stages",
                Map.of("stage", "ENDED", "endReason", "ACCEPTED_OFFER", "version", 0), 200);
        String corrected = application("SUBMITTED");
        request(HttpMethod.POST, "/applications/" + corrected + "/stages",
                Map.of("stage", "TO_APPLY", "version", 0), 200);
        var counts = get("/dashboard").path("counts");
        assertThat(counts.path("totalSubmitted").asLong()).isEqualTo(10);
        assertThat(counts.path("active").asLong()).isEqualTo(6);
        assertThat(counts.path("interviewing").asLong()).isEqualTo(4);
        assertThat(counts.path("offers").asLong()).isEqualTo(1);
        assertThat(counts.path("rejected").asLong()).isEqualTo(1);
    }

    @Test
    void dashboardLimitsListsWithoutLosingCountsAndExcludesFutureInterviews() {
        String app = application("SUBMITTED");
        String recent = interview(app, NOW.toString());
        interview(app, NOW.minusSeconds(1).toString());
        interview(app, NOW.plusSeconds(1).toString());
        interview(app, null);
        for (int i = 0; i < 7; i++) {
            todo(app, "OTHER", NOW.plusSeconds(i).toString());
            todo(app, "OTHER", NOW.minusSeconds(i + 1).toString());
        }
        todo(app, "OTHER", null);
        var dashboard = get("/dashboard");
        assertThat(dashboard.path("generatedAt").asText()).isEqualTo(NOW.toString());
        assertThat(dashboard.path("upcomingUntil").asText()).isEqualTo(NOW.plus(Duration.ofDays(7)).toString());
        assertThat(dashboard.path("upcomingTodoCount").asLong()).isEqualTo(7);
        assertThat(dashboard.path("overdueTodoCount").asLong()).isEqualTo(7);
        assertThat(dashboard.path("undatedTodoCount").asLong()).isEqualTo(1);
        assertThat(dashboard.path("upcomingTodos").size()).isEqualTo(5);
        assertThat(dashboard.path("overdueTodos").size()).isEqualTo(5);
        assertThat(dashboard.path("recentInterviews").size()).isEqualTo(2);
        assertThat(dashboard.path("recentInterviews").get(0).path("id").asText()).isEqualTo(recent);
    }

    @Test
    void sortsRecentApplicationsByActualDateWithUnknownLast() {
        String unknown = application("SUBMITTED");
        String older = application("SUBMITTED");
        String newer = application("SUBMITTED");
        request(HttpMethod.PUT, "/applications/" + older, Map.of("appliedOn", "2026-09-01", "version", 0), 200);
        request(HttpMethod.PUT, "/applications/" + newer, Map.of("appliedOn", "2026-10-01", "version", 0), 200);
        var list = get("/dashboard").path("recentApplications");
        assertThat(list.get(0).path("id").asText()).isEqualTo(newer);
        assertThat(list.get(1).path("id").asText()).isEqualTo(older);
        assertThat(list.get(2).path("id").asText()).isEqualTo(unknown);
    }

    @Test
    void validatesInputsAndSearchesLiteralWildcards() {
        String app = application("SUBMITTED");
        request(HttpMethod.POST, "/interviews", Map.of("applicationId", app, "roundName", " "), 400);
        request(HttpMethod.POST, "/interviews", Map.of("applicationId", app, "roundName", "轮次",
                "interviewAt", "2026-10-07T10:00:00"), 400);
        request(HttpMethod.POST, "/todos", Map.of("applicationId", app, "kind", "BAD", "title", "事项"), 400);
        request(HttpMethod.POST, "/todos", Map.of("applicationId", app, "kind", "OTHER", "title", "事项",
                "dueAt", "2026-10-07T10:00:00"), 400);
        request(HttpMethod.GET, "/todos?size=101", null, 400);
        request(HttpMethod.GET, "/interviews?applicationId=-1", null, 400);
        request(HttpMethod.GET, "/todos?timing=BAD", null, 400);
        request(HttpMethod.DELETE, "/todos/999999", null, 400);
        request(HttpMethod.POST, "/interviews", Map.of("applicationId", app, "roundName", "含%_轮次"), 201);
        request(HttpMethod.POST, "/todos", Map.of("applicationId", app, "kind", "OTHER", "title", "含%_事项"), 201);
        assertThat(get("/interviews?q=" + encode("%_")).path("total").asLong()).isEqualTo(1);
        assertThat(get("/todos?q=" + encode("%_")).path("total").asLong()).isEqualTo(1);
        assertThat(get("/todos?q=" + encode("' OR 1=1 --")).path("total").asLong()).isZero();
        assertThat(get("/interviews?page=2&size=1").path("items").size()).isZero();
    }

    @Test
    void concurrentInterviewUpdatesHaveOneWinner() throws Exception {
        String id = interview(application("SUBMITTED"), null);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var jobs = new ArrayList<Future<Integer>>();
            for (String name : List.of("复盘 A", "复盘 B"))
                jobs.add(pool.submit(() -> {
                    start.await();
                    return raw(HttpMethod.PUT, "/interviews/" + id,
                            Map.of("roundName", "一面", "review", name, "version", 0)).getStatusCode().value();
                }));
            start.countDown();
            assertThat(List.of(jobs.get(0).get(10, TimeUnit.SECONDS), jobs.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(get("/interviews/" + id).path("version").asInt()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentTodoEditAndCompletionHaveOneWinner() throws Exception {
        String id = todo(application("SUBMITTED"), "OTHER", null);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var edit = pool.submit(() -> { start.await(); return raw(HttpMethod.PUT, "/todos/" + id,
                    Map.of("kind", "OTHER", "title", "新标题", "version", 0)).getStatusCode().value(); });
            var complete = pool.submit(() -> { start.await(); return raw(HttpMethod.PUT, "/todos/" + id + "/completion",
                    Map.of("completed", true, "version", 0)).getStatusCode().value(); });
            start.countDown();
            assertThat(List.of(edit.get(10, TimeUnit.SECONDS), complete.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(get("/todos/" + id).path("version").asInt()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    private String application(String stage) {
        String company = request(HttpMethod.POST, "/companies", Map.of("name", "虚构测试公司", "type", "INTERNET"), 201).path("id").asText();
        String position = request(HttpMethod.POST, "/positions", Map.of("companyId", company, "name", "测试后端岗位"), 201).path("id").asText();
        return request(HttpMethod.POST, "/applications", Map.of("jobPositionId", position, "stage", stage), 201).path("id").asText();
    }
    private String interview(String app, String time) {
        var body = new HashMap<String, Object>();
        body.put("applicationId", app); body.put("roundName", "一面"); body.put("interviewAt", time);
        body.put("format", "ONLINE"); body.put("review", "虚构复盘");
        return request(HttpMethod.POST, "/interviews", body, 201).path("id").asText();
    }
    private String todo(String app, String kind, String time) {
        var body = new HashMap<String, Object>();
        body.put("applicationId", app); body.put("kind", kind); body.put("title", "测试事项");
        body.put("dueAt", time); body.put("notes", "虚构备注");
        return request(HttpMethod.POST, "/todos", body, 201).path("id").asText();
    }
    private JsonNode get(String path) { return request(HttpMethod.GET, path, null, 200); }
    private JsonNode request(HttpMethod method, String path, Object body, int status) {
        var response = raw(method, path, body);
        assertThat(response.getStatusCode().value()).as("HTTP %s %s: %s", method, path, response.getBody()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().path("data");
    }
    private ResponseEntity<JsonNode> raw(HttpMethod method, String path, Object body) {
        return http.exchange(URI.create("http://127.0.0.1:" + port + "/api/v1" + path),
                method, new HttpEntity<>(body), JsonNode.class);
    }
    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
}
