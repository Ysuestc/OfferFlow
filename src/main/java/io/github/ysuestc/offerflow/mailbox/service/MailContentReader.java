package io.github.ysuestc.offerflow.mailbox.service;

import io.github.ysuestc.offerflow.mailbox.entity.MailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.jsoup.Jsoup;

public class MailContentReader {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TEXT = 20000;

    public MailMessage read(Message source) throws MessagingException, IOException {
        MailMessage result = new MailMessage();
        result.setSubject(limit(Objects.requireNonNullElse(source.getSubject(), "（无主题）"), 1000));
        result.setSender(limit(source.getFrom() == null ? "（未知发件人）" :
                String.join(", ", Arrays.stream(source.getFrom()).map(Object::toString).toList()), 1000));
        result.setMessageId(limit(source.getHeader("Message-ID") == null ? null :
                String.join(" ", source.getHeader("Message-ID")), 998));
        result.setReceivedAt(instant(source.getReceivedDate()));
        result.setSentAt(instant(source.getSentDate()));
        result.setBodyTruncated(false);
        byte[] raw = null;
        if (source.getSize() <= MAX_BYTES) {
            try {
                var output = new LimitedOutput();
                source.writeTo(output);
                raw = output.toByteArray();
            } catch (SizeLimit e) { /* The server's size hint may be inaccurate. */ }
        }
        if (raw == null) {
            result.setContentStatus("TOO_LARGE");
            String headers = result.getMessageId() + "\n" + result.getSender() + "\n" + result.getSubject()
                    + "\n" + result.getSentAt() + "\n" + result.getReceivedAt() + "\n" + source.getSize();
            result.setFingerprint(hash(headers.getBytes(StandardCharsets.UTF_8)));
        } else {
            result.setContentStatus("AVAILABLE");
            result.setFingerprint(hash(raw));
            String text = body(new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(raw)), 0);
            result.setBodyTruncated(text.codePointCount(0, text.length()) > MAX_TEXT);
            result.setBodyText(limit(text, MAX_TEXT));
        }
        return result;
    }

    private String body(Part part, int depth) throws MessagingException, IOException {
        if (depth > 20) throw new MailFailure(MailFailure.Kind.CONTENT);
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) return "";
        if (part.isMimeType("text/plain")) return Objects.toString(part.getContent(), "");
        if (part.isMimeType("text/html")) {
            var document = Jsoup.parse(Objects.toString(part.getContent(), ""));
            document.select("script,style,noscript,iframe,object,head").remove();
            return document.body().wholeText();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            if (part.isMimeType("multipart/alternative")) {
                for (int i = 0; i < multipart.getCount(); i++)
                    if (multipart.getBodyPart(i).isMimeType("text/plain")) return body(multipart.getBodyPart(i), depth + 1);
                for (int i = 0; i < multipart.getCount(); i++) {
                    String candidate = body(multipart.getBodyPart(i), depth + 1);
                    if (!candidate.isBlank()) return candidate;
                }
                return "";
            }
            List<String> pieces = new ArrayList<>();
            for (int i = 0; i < multipart.getCount(); i++) pieces.add(body(multipart.getBodyPart(i), depth + 1));
            return String.join("\n", pieces).strip();
        }
        return "";
    }

    static String limit(String value, int max) {
        if (value == null || value.codePointCount(0, value.length()) <= max) return value;
        return value.substring(0, value.offsetByCodePoints(0, max));
    }
    private static Instant instant(Date value) { return value == null ? null : value.toInstant(); }
    private static String hash(byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    private static class LimitedOutput extends ByteArrayOutputStream {
        @Override public synchronized void write(int value) {
            if (count >= MAX_BYTES) throw new SizeLimit();
            super.write(value);
        }
        @Override public synchronized void write(byte[] value, int offset, int length) {
            if ((long) count + length > MAX_BYTES) throw new SizeLimit();
            super.write(value, offset, length);
        }
    }
    private static class SizeLimit extends RuntimeException {}
}
