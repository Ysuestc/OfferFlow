package io.github.ysuestc.offerflow.mailbox.vo;

public record MailOperation(String status, int imported, int scanned, boolean hasMore,
        String errorCode, String message) {}
