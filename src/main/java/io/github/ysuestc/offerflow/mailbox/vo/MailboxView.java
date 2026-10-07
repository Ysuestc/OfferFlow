package io.github.ysuestc.offerflow.mailbox.vo;

import io.github.ysuestc.offerflow.mailbox.entity.MailProvider;
import java.time.Instant;

public record MailboxView(String email, MailProvider provider, String folder, Instant syncFrom,
        boolean credentialConfigured, boolean identityLocked, boolean busy, int version,
        String lastStatus, Instant lastAttemptAt, Instant lastSuccessAt, String lastError,
        int lastImported, long messageCount) {}
