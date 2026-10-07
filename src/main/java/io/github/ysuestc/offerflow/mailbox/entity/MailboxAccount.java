package io.github.ysuestc.offerflow.mailbox.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @TableName("mailbox_account")
public class MailboxAccount extends BaseEntity {
    private String email;
    private MailProvider provider;
    private String folder;
    private Instant syncFrom;
    private byte[] credential;
    private Long uidValidity;
    private Long lastUid;
    private String lastStatus;
    private Instant lastAttemptAt;
    private Instant lastSuccessAt;
    private String lastError;
    private Integer lastImported;
    private Integer version;
}
