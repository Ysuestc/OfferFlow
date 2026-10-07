package io.github.ysuestc.offerflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql")
class MySqlWorkspaceIT {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        var database = MySqlTestSettings.workspace();
        database.requireEmptySchema();
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
    }

    @BeforeEach
    void emptyOwnedSchema() {
        for (String table : List.of("todo", "interview", "application_stage_history", "application", "job_position", "company")) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    @Test
    void createsEditsSearchesAndPagesRealCatalogs() {
        String company = company(" 星河科技🌟 ");
        assertThat(get("/companies/" + company).path("name").asText()).isEqualTo("星河科技🌟");
        company("另一家公司");
        JsonNode page = get("/companies?page=2&size=1");
        assertThat(page.path("total").asLong()).isEqualTo(2);
        assertThat(page.path("items").get(0).path("id").asText()).isEqualTo(company);
        String position = position(company, "Java 后端");
        assertThat(get("/positions?q=" + encode("星河")).path("total").asLong()).isEqualTo(1);
        assertThat(get("/positions/" + position).path("companyId").isTextual()).isTrue();
        assertThat(get("/positions?companyId=" + company).path("items").get(0).path("jd").asText()).contains("职责");
        request(HttpMethod.PUT, "/companies/" + company, Map.of("name", "星河实验室", "type", "RESEARCH_INSTITUTE"), 200);
        assertThat(get("/companies/" + company).path("website").isNull()).isTrue();
        assertThat(get("/positions/" + position).path("companyName").asText()).isEqualTo("星河实验室");
        request(HttpMethod.PUT, "/positions/" + position, Map.of("companyId", company, "name", "Agent 开发"), 200);
        assertThat(get("/positions/" + position).path("jd").isNull()).isTrue();
    }

    @Test
    void treatsSqlWildcardsAsLiteralSearchText() {
        company("A%_科技");
        company("ABC科技");
        assertThat(get("/companies?q=" + encode("%_")).path("total").asLong()).isEqualTo(1);
        assertThat(get("/companies?q=" + encode("' OR 1=1 --")).path("total").asLong()).isZero();
    }

    @Test
    void rejectsInvalidInputsAndMissingRelationsSafely() {
        request(HttpMethod.POST, "/companies", Map.of("name", " ", "type", "BANK"), 400);
        request(HttpMethod.POST, "/companies", Map.of("name", "公司", "type", "BANK", "website", "javascript:alert(1)"), 400);
        request(HttpMethod.POST, "/positions", Map.of("companyId", "999999", "name", "岗位"), 404);
        request(HttpMethod.GET, "/companies?size=101", null, 400);
        request(HttpMethod.GET, "/applications?stage=UNKNOWN", null, 400);
        assertThat(get("/companies").path("total").asLong()).isZero();
    }

    @Test
    void protectsReferencesAndAllowsUnusedDeletion() {
        String company = company("公司");
        String position = position(company, "岗位");
        request(HttpMethod.DELETE, "/companies/" + company, null, 409);
        String app = application(position, "SUBMITTED");
        request(HttpMethod.DELETE, "/positions/" + position, null, 409);
        assertThat(get("/applications/" + app).path("positionName").asText()).isEqualTo("岗位");
        String unused = company("尚无岗位");
        request(HttpMethod.DELETE, "/companies/" + unused, null, 200);
        request(HttpMethod.GET, "/companies/" + unused, null, 404);
    }

    @Test
    void preservesInitialHistoryAndRejectsDuplicateApplications() {
        String position = position(company("公司"), "后端");
        String id = application(position, "SECOND_INTERVIEW");
        JsonNode app = get("/applications/" + id);
        assertThat(app.path("submitted").asBoolean()).isTrue();
        assertThat(app.path("appliedOn").isNull()).isTrue();
        assertThat(history(id).size()).isEqualTo(1);
        assertThat(history(id).get(0).path("stageOn").isNull()).isTrue();
        request(HttpMethod.POST, "/applications", Map.of("jobPositionId", position, "stage", "SUBMITTED"), 409);
        assertThat(get("/applications").path("total").asLong()).isEqualTo(1);
        assertThat(history(id).size()).isEqualTo(1);
    }

    @Test
    void jumpsCorrectsAndEndsWithoutErasingHistoryOrFacts() {
        String id = application(position(company("公司"), "岗位"), "TO_APPLY");
        stage(id, "SECOND_INTERVIEW", "2026-09-29", null, 0, 200);
        JsonNode app = get("/applications/" + id);
        assertThat(app.path("submitted").asBoolean()).isTrue();
        assertThat(app.path("appliedOn").isNull()).isTrue();
        stage(id, "TO_APPLY", null, null, 1, 200);
        assertThat(get("/applications/" + id).path("submitted").asBoolean()).isTrue();
        stage(id, "ENDED", null, "WITHDRAWN", 2, 200);
        assertThat(get("/applications/" + id).path("endReason").asText()).isEqualTo("WITHDRAWN");
        assertThat(history(id).size()).isEqualTo(4);
        assertThat(history(id).get(1).path("stageOn").asText()).isEqualTo("2026-09-29");
        assertThat(history(id).get(0).path("stage").asText()).isEqualTo("TO_APPLY");
    }

    @Test
    void validatesEndMeaningAndUnsubmittedDates() {
        String position = position(company("公司"), "岗位");
        request(HttpMethod.POST, "/applications", Map.of("jobPositionId", position, "stage", "TO_APPLY",
                "appliedOn", "2026-09-20"), 400);
        String id = application(position, "SUBMITTED");
        stage(id, "ENDED", null, null, 0, 400);
        stage(id, "REJECTED", null, "DECLINED_OFFER", 0, 400);
        assertThat(history(id).size()).isEqualTo(1);
        assertThat(get("/applications/" + id).path("version").asInt()).isZero();
        stage(id, "REJECTED", null, null, 0, 200);
        assertThat(get("/applications/" + id).path("endReason").isNull()).isTrue();
    }

    @Test
    void clearsApplicationOptionalValuesAndRejectsStaleMetadata() {
        String id = application(position(company("公司"), "岗位"), "SUBMITTED");
        request(HttpMethod.PUT, "/applications/" + id, Map.of("channel", "内推", "appliedOn", "2026-09-20",
                "notes", "复习 Java", "version", 0), 200);
        request(HttpMethod.PUT, "/applications/" + id, Map.of("version", 0, "notes", "陈旧"), 409);
        request(HttpMethod.PUT, "/applications/" + id, Map.of("version", 1), 200);
        JsonNode app = get("/applications/" + id);
        assertThat(app.path("channel").isNull()).isTrue();
        assertThat(app.path("appliedOn").isNull()).isTrue();
        assertThat(app.path("notes").isNull()).isTrue();
        assertThat(history(id).size()).isEqualTo(1);
    }

    @Test
    void repeatsIdenticalStageWithoutDuplicatingHistory() {
        String id = application(position(company("公司"), "岗位"), "SUBMITTED");
        stage(id, "SUBMITTED", null, null, 0, 200);
        assertThat(history(id).size()).isEqualTo(1);
        assertThat(get("/applications/" + id).path("version").asInt()).isZero();
        stage(id, "OFFER", null, null, 0, 200);
        stage(id, "OFFER", null, null, 0, 409);
        assertThat(history(id).size()).isEqualTo(2);
    }

    @Test
    void concurrentStageWritersCannotOverwriteEachOther() throws Exception {
        String id = application(position(company("公司"), "岗位"), "SUBMITTED");
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = executor.submit(() -> { start.await(); return raw(HttpMethod.POST, "/applications/" + id + "/stages",
                    Map.of("stage", "FIRST_INTERVIEW", "version", 0)).getStatusCode().value(); });
            var second = executor.submit(() -> { start.await(); return raw(HttpMethod.POST, "/applications/" + id + "/stages",
                    Map.of("stage", "SECOND_INTERVIEW", "version", 0)).getStatusCode().value(); });
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(history(id).size()).isEqualTo(2);
        assertThat(get("/applications/" + id).path("version").asInt()).isEqualTo(1);
    }

    @Test
    void historyFailureRollsBackSnapshotInRealHttpTransaction() {
        String id = application(position(company("公司"), "岗位"), "SUBMITTED");
        jdbc.execute("CREATE TRIGGER workspace_reject_history BEFORE INSERT ON application_stage_history "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'private failure'");
        try {
            ResponseEntity<JsonNode> response = raw(HttpMethod.POST, "/applications/" + id + "/stages",
                    Map.of("stage", "OFFER", "version", 0));
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().toString()).doesNotContain("private failure", "INSERT", "SQLException");
            assertThat(get("/applications/" + id).path("currentStage").asText()).isEqualTo("SUBMITTED");
            assertThat(get("/applications/" + id).path("version").asInt()).isZero();
            assertThat(history(id).size()).isEqualTo(1);
        } finally {
            jdbc.execute("DROP TRIGGER workspace_reject_history");
        }
    }

    @Test
    void listsApplicationFiltersAndWorkspaceAvailability() {
        String company = company("星河");
        application(position(company, "Java"), "FIRST_INTERVIEW");
        application(position(company, "Agent"), "OFFER");
        assertThat(get("/workspace").path("available").asBoolean()).isTrue();
        assertThat(get("/applications?stage=OFFER&q=" + encode("星河")).path("total").asLong()).isEqualTo(1);
        assertThat(get("/applications?page=2&size=1").path("items").size()).isEqualTo(1);
    }

    private String company(String name) {
        return request(HttpMethod.POST, "/companies", Map.of("name", name, "type", "INTERNET",
                "website", "https://example.org", "notes", "示例备注"), 201).path("id").asText();
    }
    private String position(String company, String name) {
        return request(HttpMethod.POST, "/positions", Map.of("companyId", company, "name", name,
                "location", "上海", "direction", "Java 后端", "jd", "岗位职责", "recruitmentBatch", "2026 秋招"), 201).path("id").asText();
    }
    private String application(String position, String stage) {
        return request(HttpMethod.POST, "/applications", Map.of("jobPositionId", position, "stage", stage), 201).path("id").asText();
    }
    private JsonNode history(String id) { return get("/applications/" + id + "/history"); }
    private void stage(String id, String stage, String date, String reason, int version, int expected) {
        Map<String, Object> body = new HashMap<>();
        body.put("stage", stage); body.put("stageOn", date); body.put("endReason", reason);
        body.put("version", version); body.put("remark", "人工记录");
        request(HttpMethod.POST, "/applications/" + id + "/stages", body, expected);
    }
    private JsonNode get(String path) { return request(HttpMethod.GET, path, null, 200); }
    private JsonNode request(HttpMethod method, String path, Object body, int status) {
        ResponseEntity<JsonNode> response = raw(method, path, body);
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
