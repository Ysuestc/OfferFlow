package io.github.ysuestc.offerflow.mailbox.service;

public final class MailFailure extends RuntimeException {
    public enum Kind {
        AUTHENTICATION("认证失败，请检查授权码及网易邮箱 IMAP 开启状态"),
        FOLDER("文件夹不可读取，请检查文件夹名称"),
        NETWORK("邮箱连接或读取失败，请稍后重试"),
        CONTENT("邮件内容读取失败，可重新同步重试"),
        STORAGE("邮件保存失败，已提交记录保留，请重新同步"),
        CREDENTIAL("本机密钥不可用，请重新输入授权码并保存");
        private final String message;
        Kind(String message) { this.message = message; }
        public String message() { return message; }
    }
    private final Kind kind;
    public MailFailure(Kind kind) { super(kind.message()); this.kind = kind; }
    public Kind kind() { return kind; }
}
