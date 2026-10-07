package io.github.ysuestc.offerflow.mailbox.entity;

public enum MailProvider {
    NETEASE_163("163.com"), NETEASE_126("126.com"), NETEASE_YEAH("yeah.net");
    private final String domain;
    MailProvider(String domain) { this.domain = domain; }
    public String domain() { return domain; }
    public String host() { return "imap." + domain; }
}
