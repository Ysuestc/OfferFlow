package io.github.ysuestc.offerflow.mailbox.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @TableName("mail_message")
public class MailMessage extends BaseEntity {
    private Long accountId;
    private Long uidValidity;
    private Long uid;
    private String fingerprint;
    private String messageId;
    private String subject;
    private String sender;
    private Instant receivedAt;
    private Instant sentAt;
    private String bodyText;
    private String contentStatus;
    private Boolean bodyTruncated;
}
