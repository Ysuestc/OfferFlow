package io.github.ysuestc.offerflow.mailbox.service;

import io.github.ysuestc.offerflow.mailbox.entity.MailboxAccount;
import io.github.ysuestc.offerflow.mailbox.entity.MailMessage;
import java.util.function.BiConsumer;

public interface MailClient {
    record Cursor(long validity, long uid) {}
    record ScanResult(Cursor cursor, boolean hasMore) {}
    void test(MailboxAccount account, String authorizationCode);
    ScanResult scan(MailboxAccount account, String authorizationCode,
                    BiConsumer<Cursor, MailMessage> persist);
}
