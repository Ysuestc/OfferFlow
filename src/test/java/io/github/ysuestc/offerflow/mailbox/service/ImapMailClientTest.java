package io.github.ysuestc.offerflow.mailbox.service;

import static org.assertj.core.api.Assertions.*;
import com.icegreen.greenmail.util.*;
import io.github.ysuestc.offerflow.mailbox.entity.*;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

class ImapMailClientTest {
    GreenMail server;
    ImapMailClient client;
    MailboxAccount account;
    @BeforeEach void start() {
        server = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_IMAPS));
        server.start(); server.setUser("test@163.com", "DemoAuthCode12345");
        client = new ImapMailClient() {
            @Override protected String host(MailboxAccount a) { return "127.0.0.1"; }
            @Override protected int port() { return server.getImaps().getPort(); }
            @Override protected Properties properties() {
                var p = super.properties(); p.setProperty("mail.imaps.ssl.trust", "127.0.0.1");
                p.setProperty("mail.imaps.ssl.checkserveridentity", "false"); return p;
            }
        };
        account = new MailboxAccount(); account.setEmail("test@163.com"); account.setProvider(MailProvider.NETEASE_163);
        account.setFolder("INBOX"); account.setSyncFrom(Instant.parse("2020-01-01T00:00:00Z"));
    }
    @AfterEach void stop() { server.stop(); }
    void deliver(String subject) throws Exception {
        var m = new MimeMessage(Session.getInstance(new Properties()));
        m.setFrom("hr@example.invalid"); m.setRecipients(Message.RecipientType.TO, "test@163.com");
        m.setSubject(subject, "UTF-8"); m.setText("虚构招聘邮件正文", "UTF-8"); m.saveChanges();
        server.getUserManager().getUser("test@163.com").deliver(m);
    }
    @Test void readsViaTlsIncrementallyWithoutChangingUnreadFlags() throws Exception {
        deliver("第一封"); client.test(account, "DemoAuthCode12345");
        List<MailMessage> saved = new ArrayList<>();
        var first = client.scan(account, "DemoAuthCode12345", (cursor, m) -> saved.add(m));
        assertThat(saved).hasSize(1); assertThat(saved.getFirst().getBodyText()).isEqualTo("虚构招聘邮件正文");
        account.setUidValidity(first.cursor().validity()); account.setLastUid(first.cursor().uid());
        client.scan(account, "DemoAuthCode12345", (cursor, m) -> fail("Previously read mail must not be fetched"));
        try (Store store = Session.getInstance(client.properties()).getStore("imaps")) {
            store.connect("127.0.0.1", server.getImaps().getPort(), "test@163.com", "DemoAuthCode12345");
            Folder folder = store.getFolder("INBOX"); folder.open(Folder.READ_ONLY);
            assertThat(folder.getMessage(1).isSet(Flags.Flag.SEEN)).isFalse();
            assertThat(folder.getMessage(1).isSet(Flags.Flag.DELETED)).isFalse(); folder.close(false);
        }
    }
    @Test void doesNotSkipMailArrivingDuringBodyReads() throws Exception {
        deliver("第一封");
        var first = client.scan(account, "DemoAuthCode12345", (cursor, m) -> {
            try { deliver("同步期间的新邮件"); } catch (Exception e) { throw new AssertionError(e); }
        });
        account.setUidValidity(first.cursor().validity()); account.setLastUid(first.cursor().uid());
        var subjects = new ArrayList<String>();
        client.scan(account, "DemoAuthCode12345", (cursor, m) -> subjects.add(m.getSubject()));
        assertThat(subjects).containsExactly("同步期间的新邮件");
    }
    @Test void reportsAuthenticationAndFolderFailuresSafely() {
        assertThatThrownBy(() -> client.test(account, "InvalidDemo123456")).isInstanceOf(MailFailure.class)
                .hasMessage(MailFailure.Kind.AUTHENTICATION.message()).hasNoCause();
        account.setFolder("missing-folder");
        assertThatThrownBy(() -> client.test(account, "DemoAuthCode12345")).isInstanceOf(MailFailure.class)
                .hasMessage(MailFailure.Kind.FOLDER.message()).hasNoCause();
    }
    @Test void excludesMailBeforeSyncStartAndCapsEachBatch() throws Exception {
        for (int i = 0; i < 102; i++) deliver("批次邮件 " + i);
        var subjects = new ArrayList<String>();
        var first = client.scan(account, "DemoAuthCode12345", (cursor, m) -> subjects.add(m.getSubject()));
        assertThat(subjects).hasSize(100); assertThat(first.hasMore()).isTrue();
        account.setUidValidity(first.cursor().validity()); account.setLastUid(first.cursor().uid()); subjects.clear();
        var second = client.scan(account, "DemoAuthCode12345", (cursor, m) -> subjects.add(m.getSubject()));
        assertThat(subjects).hasSize(2); assertThat(second.hasMore()).isFalse();
        account.setUidValidity(null); account.setLastUid(null); account.setSyncFrom(Instant.now().plusSeconds(60));
        client.scan(account, "DemoAuthCode12345", (cursor, m) -> assertThat(m).isNull());
    }
}
