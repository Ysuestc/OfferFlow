package io.github.ysuestc.offerflow.mailbox.service;

import static org.assertj.core.api.Assertions.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class MailContentReaderTest {
    MimeMessage message() throws Exception {
        var m = new MimeMessage(Session.getInstance(new Properties()));
        m.setFrom("recruitment@example.invalid"); m.setSubject("虚构二面邀请🌟", "UTF-8");
        return m;
    }
    @Test void readsUnicodePlainTextAndBoundsWithoutBreakingSurrogatePairs() throws Exception {
        var m = message(); m.setText("面试安排🌟".repeat(5000), "UTF-8"); m.saveChanges();
        var result = new MailContentReader().read(m);
        assertThat(result.getSubject()).isEqualTo("虚构二面邀请🌟");
        assertThat(result.getBodyTruncated()).isTrue();
        assertThat(result.getBodyText().codePointCount(0, result.getBodyText().length())).isEqualTo(20000);
        assertThat(MailContentReader.limit("a🌟b", 2)).isEqualTo("a🌟");
    }
    @Test void prefersPlainAlternativeAndSkipsAttachments() throws Exception {
        var alternative = new MimeMultipart("alternative");
        var plain = new MimeBodyPart(); plain.setText("真实文本内容", "UTF-8"); alternative.addBodyPart(plain);
        var html = new MimeBodyPart(); html.setContent("<p>另一版本</p>", "text/html;charset=UTF-8"); alternative.addBodyPart(html);
        var container = new MimeBodyPart(); container.setContent(alternative);
        var mixed = new MimeMultipart("mixed"); mixed.addBodyPart(container);
        var attachment = new MimeBodyPart(); attachment.setText("附件里的误导文字", "UTF-8");
        attachment.setFileName("fake.txt"); mixed.addBodyPart(attachment);
        var m = message(); m.setContent(mixed); m.saveChanges();
        assertThat(new MailContentReader().read(m).getBodyText()).isEqualTo("真实文本内容");
    }
    @Test void convertsHtmlIntoInertText() throws Exception {
        var m = message(); m.setContent("<html><head><style>hidden</style></head><body><p>邀请面试</p><script>alert(1)</script><img src='https://example.invalid/tracker'><a href='javascript:alert(2)'>确认时间</a></body></html>", "text/html;charset=UTF-8"); m.saveChanges();
        String body = new MailContentReader().read(m).getBodyText();
        assertThat(body).contains("邀请面试", "确认时间").doesNotContain("script", "alert", "tracker", "hidden");
    }
    @Test void storesHeadersOnlyWhenActualBytesExceedLimit() throws Exception {
        var m = message(); m.setText("x".repeat(MailContentReader.MAX_BYTES + 100), "UTF-8"); m.saveChanges();
        var result = new MailContentReader().read(m);
        assertThat(result.getContentStatus()).isEqualTo("TOO_LARGE");
        assertThat(result.getBodyText()).isNull();
        assertThat(result.getFingerprint()).hasSize(64);
    }
}
