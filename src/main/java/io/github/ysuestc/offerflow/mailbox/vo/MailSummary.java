package io.github.ysuestc.offerflow.mailbox.vo;

import io.github.ysuestc.offerflow.mailbox.entity.MailMessage;
import java.time.Instant;

public record MailSummary(String id, String subject, String sender, Instant receivedAt,
        Instant sentAt, String contentStatus, boolean bodyTruncated) {
    public static MailSummary from(MailMessage m) {
        return new MailSummary(m.getId().toString(), m.getSubject(), m.getSender(),
                m.getReceivedAt(), m.getSentAt(), m.getContentStatus(), m.getBodyTruncated());
    }
}
