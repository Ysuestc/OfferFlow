package io.github.ysuestc.offerflow.persistence;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.ysuestc.offerflow.mailbox.entity.*;
import io.github.ysuestc.offerflow.mailbox.service.*;
import java.time.Instant;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql") @Import(MySqlMailboxIT.LocalClient.class)
class MySqlMailboxIT {
    @TempDir static Path directory;
    @TestConfiguration static class LocalClient {
        @Bean @Primary FakeClient fakeClient() { return new FakeClient(); }
    }
    static class FakeClient implements MailClient {
        long validity = 10;
        int size = 3;
        volatile MailFailure.Kind testFailure;
        int failAfter;
        volatile CountDownLatch entered, release;
        @Override public void test(MailboxAccount a, String code) {
            assertThat(code).isEqualTo("DemoAuthCode12345");
            if (testFailure != null) throw new MailFailure(testFailure);
        }
        @Override public ScanResult scan(MailboxAccount a, String code, BiConsumer<Cursor, MailMessage> persist) {
            if (entered != null) {
                entered.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Test latch timed out"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            }
            long start = Objects.equals(a.getUidValidity(), validity) && a.getLastUid() != null ? a.getLastUid() + 1 : 1;
            for (long i = start; i <= size; i++) {
                MailMessage m = new MailMessage();
                m.setFingerprint(String.format("%064d", i)); m.setMessageId("<fake-" + i + "@example.invalid>");
                m.setSubject(i == 1 ? "虚构二面邀请 100%_" : "虚构通知 " + i);
                m.setSender("虚构 HR <hr@example.invalid>"); m.setReceivedAt(Instant.parse("2026-10-07T02:00:00Z"));
                m.setBodyText("<script>alert('fake')</script> 邮件作为文本展示"); m.setContentStatus("AVAILABLE");
                m.setBodyTruncated(false); persist.accept(new Cursor(validity, i), m);
                if (i == failAfter) throw new MailFailure(MailFailure.Kind.NETWORK);
            }
            return new ScanResult(new Cursor(validity, size), false);
        }
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired FakeClient client;
    @Autowired CredentialVault vault;
    @LocalServerPort int port;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        var database = MySqlTestSettings.mailbox(); database.requireEmptySchema();
        registry.add("spring.datasource.url", database::url); registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("offerflow.mailbox.private-dir", () -> directory.resolve("vault").toString());
    }
    @BeforeEach void reset() {
        jdbc.execute("DROP TRIGGER IF EXISTS fail_mail_cursor");
        jdbc.update("DELETE FROM mail_message"); jdbc.update("DELETE FROM mailbox_account");
        for (String table : core()) jdbc.update("DELETE FROM " + table);
        client.validity = 10; client.size = 3; client.failAfter = 0;
        client.testFailure = null; client.entered = null; client.release = null;
    }
    List<String> core() { return List.of("todo", "interview", "application_stage_history", "application", "job_position", "company"); }
    Map<String, Object> settings(int version) {
        return new HashMap<>(Map.of("email", "test@163.com", "provider", "NETEASE_163", "folder", "INBOX",
                "syncFrom", "2020-01-01T00:00:00Z", "authorizationCode", "DemoAuthCode12345", "version", version));
    }
    JsonNode call(HttpMethod method, String path, Object body, int expected) {
        var response = http.exchange(java.net.URI.create("http://127.0.0.1:" + port + "/api/v1" + path), method, new HttpEntity<>(body), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(expected);
        if (path.startsWith("/mailbox")) assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody().toString()).doesNotContain("DemoAuthCode12345", "credential\"", "mail.key", "SQLException");
        return response.getBody().path("data");
    }
    JsonNode get(String path) { return call(HttpMethod.GET, path, null, 200); }
    void save() { call(HttpMethod.PUT, "/mailbox", settings(0), 200); }
    JsonNode sync() { return call(HttpMethod.POST, "/mailbox/syncs", null, 200); }

    @Test void storesEncryptedCredentialAndSupportsVersionedRotation() {
        assertThat(get("/mailbox").isNull()).isTrue(); save();
        byte[] encrypted = jdbc.queryForObject("SELECT credential FROM mailbox_account", byte[].class);
        assertThat(new String(encrypted, java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("DemoAuthCode12345");
        assertThat(vault.decrypt(encrypted)).isEqualTo("DemoAuthCode12345");
        assertThat(new CredentialVault(directory.resolve("vault").toString()).decrypt(encrypted)).isEqualTo("DemoAuthCode12345");
        assertThat(get("/mailbox").path("busy").asBoolean()).isFalse();
        call(HttpMethod.PUT, "/mailbox", settings(0), 409);
        var rotation = settings(1); rotation.remove("authorizationCode");
        assertThat(call(HttpMethod.PUT, "/mailbox", rotation, 200).path("version").asInt()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT credential FROM mailbox_account", byte[].class)).isEqualTo(encrypted);
    }
    @Test void validatesSettingsAndMissingResources() {
        call(HttpMethod.POST, "/mailbox/syncs", null, 404);
        call(HttpMethod.POST, "/mailbox/connection-tests", null, 404);
        var request = settings(0); request.remove("authorizationCode"); call(HttpMethod.PUT, "/mailbox", request, 400);
        request = settings(0); request.put("provider", "NETEASE_126"); call(HttpMethod.PUT, "/mailbox", request, 400);
        request = settings(0); request.put("folder", "INBOX\r\nBAD"); call(HttpMethod.PUT, "/mailbox", request, 400);
        request = settings(0); request.put("syncFrom", "2999-01-01T00:00:00Z"); call(HttpMethod.PUT, "/mailbox", request, 400);
        call(HttpMethod.GET, "/mailbox/messages?size=101", null, 400);
        call(HttpMethod.GET, "/mailbox/messages/99999999", null, 404);
    }
    @Test void reportsConnectionFailuresWithoutProtocolData() {
        save(); assertThat(call(HttpMethod.POST, "/mailbox/connection-tests", null, 200).path("status").asText()).isEqualTo("CONNECTED");
        client.testFailure = MailFailure.Kind.AUTHENTICATION;
        assertThat(call(HttpMethod.POST, "/mailbox/connection-tests", null, 200).path("errorCode").asText()).isEqualTo("AUTHENTICATION");
    }
    @Test void deduplicatesUidResetSearchesLiterallyAndNeverTouchesBusinessTables() {
        var app = call(HttpMethod.POST, "/applications/quick", Map.of("companyName", "邮箱隔离验收公司", "positionName", "Java",
                "stage", "SECOND_INTERVIEW"), 201).path("id").asText();
        call(HttpMethod.POST, "/interviews", Map.of("applicationId", app, "roundName", "二面"), 201);
        call(HttpMethod.POST, "/todos", Map.of("applicationId", app, "kind", "INTERVIEW", "title", "原有待办"), 201);
        var before = core().stream().map(t -> jdbc.queryForList("SELECT * FROM " + t + " ORDER BY id")).toList();
        save(); assertThat(sync().path("imported").asInt()).isEqualTo(3); assertThat(sync().path("imported").asInt()).isZero();
        client.validity = 20; assertThat(sync().path("imported").asInt()).isZero();
        assertThat(jdbc.queryForObject("SELECT uid_validity FROM mailbox_account", Long.class)).isEqualTo(20);
        assertThat(core().stream().map(t -> jdbc.queryForList("SELECT * FROM " + t + " ORDER BY id")).toList()).isEqualTo(before);
        assertThat(get("/mailbox/messages?q=100%25_").path("total").asInt()).isEqualTo(1);
        var page = get("/mailbox/messages?page=2&size=2"); assertThat(page.path("items").size()).isEqualTo(1);
        assertThat(page.path("items").get(0).has("bodyText")).isFalse();
        String id = page.path("items").get(0).path("id").asText();
        assertThat(get("/mailbox/messages/" + id).path("bodyText").asText()).contains("邮件作为文本展示");
        var modified = settings(1); modified.put("folder", "another"); call(HttpMethod.PUT, "/mailbox", modified, 409);
        call(HttpMethod.PUT, "/mailbox", settings(1), 200);
    }
    @Test void rollsBackMailAndCursorTogetherThenResumesAfterPartialFailure() {
        save();
        jdbc.execute("""
            CREATE TRIGGER fail_mail_cursor BEFORE UPDATE ON mailbox_account FOR EACH ROW
            BEGIN IF NEW.last_uid = 2 AND NOT (NEW.last_uid <=> OLD.last_uid) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fake cursor failure'; END IF; END
            """);
        var failed = sync(); assertThat(failed.path("status").asText()).isEqualTo("PARTIAL");
        assertThat(failed.path("imported").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mail_message", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT last_uid FROM mailbox_account", Long.class)).isEqualTo(1);
        jdbc.execute("DROP TRIGGER fail_mail_cursor");
        assertThat(sync().path("imported").asInt()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mail_message", Integer.class)).isEqualTo(3);
        assertThat(get("/mailbox").path("lastStatus").asText()).isEqualTo("SUCCESS");
    }
    @Test void rejectsConcurrentOperationsAndReleasesGate() throws Exception {
        save(); client.entered = new CountDownLatch(1); client.release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var pending = pool.submit(this::sync);
            try {
                assertThat(client.entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(get("/mailbox").path("busy").asBoolean()).isTrue();
                call(HttpMethod.POST, "/mailbox/syncs", null, 409);
                call(HttpMethod.POST, "/mailbox/connection-tests", null, 409);
                call(HttpMethod.PUT, "/mailbox", settings(1), 409);
            } finally { client.release.countDown(); }
            assertThat(pending.get(5, TimeUnit.SECONDS).path("status").asText()).isEqualTo("SUCCESS");
        }
        assertThat(get("/mailbox").path("busy").asBoolean()).isFalse();
    }
    @Test void resumesAfterNetworkFailureAndStaleRunningState() {
        save(); client.failAfter = 1;
        var partial = sync(); assertThat(partial.path("status").asText()).isEqualTo("PARTIAL");
        assertThat(partial.path("errorCode").asText()).isEqualTo("NETWORK");
        assertThat(jdbc.queryForObject("SELECT last_uid FROM mailbox_account", Long.class)).isEqualTo(1);
        assertThat(get("/mailbox").path("lastSuccessAt").isNull()).isTrue();
        client.failAfter = 0; jdbc.update("UPDATE mailbox_account SET last_status = 'RUNNING'");
        assertThat(sync().path("imported").asInt()).isEqualTo(2);
        assertThat(get("/mailbox").path("lastSuccessAt").isNull()).isFalse();
    }
    @Test void missingKeyFailsSafelyAndExplicitCredentialReplacementRecovers() throws Exception {
        save(); java.nio.file.Files.delete(directory.resolve("vault/mail.key"));
        assertThat(sync().path("errorCode").asText()).isEqualTo("CREDENTIAL");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mail_message", Integer.class)).isZero();
        call(HttpMethod.PUT, "/mailbox", settings(1), 200);
        assertThat(sync().path("imported").asInt()).isEqualTo(3);
    }
    @Test void enforcesSingletonForeignKeysAndUidUniquenessInMySql() {
        save(); sync();
        assertThatThrownBy(() -> jdbc.update("UPDATE mailbox_account SET id = 2")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE mailbox_account SET version = -1")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM mailbox_account")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE mail_message SET uid = 0")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE mail_message SET uid = 1 WHERE uid = 2")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE mail_message SET content_status = 'available'")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
