package io.github.ysuestc.offerflow.mailbox.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.mailbox.dto.MailboxRequest;
import io.github.ysuestc.offerflow.mailbox.entity.*;
import io.github.ysuestc.offerflow.mailbox.mapper.*;
import io.github.ysuestc.offerflow.mailbox.vo.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service @Profile("mysql")
public class MailboxService {
    private final MailboxAccountMapper accounts;
    private final MailMessageMapper messages;
    private final CredentialVault vault;
    private final MailClient client;
    private final Clock clock;
    private final TransactionTemplate transaction;
    private final ReentrantLock gate = new ReentrantLock();

    public MailboxService(MailboxAccountMapper accounts, MailMessageMapper messages, CredentialVault vault,
            MailClient client, Clock clock, PlatformTransactionManager manager) {
        this.accounts = accounts; this.messages = messages; this.vault = vault;
        this.client = client; this.clock = clock; transaction = new TransactionTemplate(manager);
    }

    public MailboxView get() {
        MailboxAccount a = accounts.selectById(1);
        if (a == null) return null;
        long count = messages.selectCount(Wrappers.<MailMessage>lambdaQuery().eq(MailMessage::getAccountId, 1));
        return new MailboxView(a.getEmail(), a.getProvider(), a.getFolder(), a.getSyncFrom(),
                a.getCredential() != null, count > 0, gate.isLocked() && !gate.isHeldByCurrentThread(), a.getVersion(), a.getLastStatus(),
                a.getLastAttemptAt(), a.getLastSuccessAt(), a.getLastError(), a.getLastImported(), count);
    }

    public MailboxView save(MailboxRequest request) {
        return exclusively(() -> transaction.execute(status -> {
            String email = request.email().strip().toLowerCase(Locale.ROOT);
            String folder = request.folder().strip();
            if (!email.endsWith("@" + request.provider().domain()) || folder.chars().anyMatch(Character::isISOControl))
                throw new BusinessException(ApiErrorCode.BAD_REQUEST, "邮箱后缀须匹配提供商，文件夹名称不能含控制字符");
            var from = request.syncFrom().truncatedTo(ChronoUnit.MICROS);
            MailboxAccount a = accounts.selectById(1);
            if ((a == null ? 0 : a.getVersion()) != request.version())
                throw new BusinessException(ApiErrorCode.CONFLICT, "邮箱设置已更新，请刷新后操作");
            boolean changed = a == null || !Objects.equals(email, a.getEmail()) || request.provider() != a.getProvider()
                    || !Objects.equals(folder, a.getFolder()) || !Objects.equals(from, a.getSyncFrom());
            if (changed && messages.selectCount(null) > 0)
                throw new BusinessException(ApiErrorCode.CONFLICT, "已有采集邮件，邮箱、文件夹和同步起点不能修改；可更新授权码");
            String code = request.authorizationCode() == null ? "" : request.authorizationCode().replaceAll("\\s", "");
            if ((a == null && code.isEmpty()) || (!code.isEmpty() && !code.matches("[A-Za-z0-9]{8,128}")))
                throw new BusinessException(ApiErrorCode.BAD_REQUEST, "请输入网易邮箱客户端授权码，不能使用邮箱登录密码");
            byte[] credential;
            try { credential = code.isEmpty() ? a.getCredential() : vault.encrypt(code); }
            catch (MailFailure failure) { throw new BusinessException(ApiErrorCode.BAD_REQUEST, failure.getMessage()); }
            if (a == null) {
                a = new MailboxAccount(); a.setId(1L); a.setEmail(email); a.setProvider(request.provider());
                a.setFolder(folder); a.setSyncFrom(from); a.setCredential(credential); a.setVersion(1);
                accounts.insert(a);
            } else {
                var update = Wrappers.<MailboxAccount>lambdaUpdate().eq(MailboxAccount::getId, 1)
                        .eq(MailboxAccount::getVersion, request.version())
                        .set(MailboxAccount::getEmail, email).set(MailboxAccount::getProvider, request.provider())
                        .set(MailboxAccount::getFolder, folder).set(MailboxAccount::getSyncFrom, from)
                        .set(MailboxAccount::getCredential, credential).set(MailboxAccount::getVersion, request.version() + 1);
                if (changed) update.set(MailboxAccount::getUidValidity, null).set(MailboxAccount::getLastUid, null)
                        .set(MailboxAccount::getLastStatus, "NEVER").set(MailboxAccount::getLastError, null)
                        .set(MailboxAccount::getLastAttemptAt, null).set(MailboxAccount::getLastSuccessAt, null)
                        .set(MailboxAccount::getLastImported, 0);
                if (accounts.update(null, update) != 1) throw new BusinessException(ApiErrorCode.CONFLICT);
            }
            return get();
        }));
    }

    public MailOperation testConnection() {
        return exclusively(() -> {
            MailboxAccount account = require();
            try {
                client.test(account, vault.decrypt(account.getCredential()));
                return new MailOperation("CONNECTED", 0, 0, false, null, "连接成功，指定文件夹可读取");
            } catch (MailFailure failure) { return failure("FAILED", 0, 0, failure.kind()); }
        });
    }

    public MailOperation sync() {
        return exclusively(() -> {
            MailboxAccount account = require();
            transaction.executeWithoutResult(tx -> accounts.update(null, Wrappers.<MailboxAccount>lambdaUpdate()
                    .eq(MailboxAccount::getId, 1).set(MailboxAccount::getLastStatus, "RUNNING")
                    .set(MailboxAccount::getLastAttemptAt, clock.instant()).set(MailboxAccount::getLastError, null)
                    .set(MailboxAccount::getLastImported, 0)));
            int[] counts = new int[2];
            MailOperation result;
            try {
                var scan = client.scan(account, vault.decrypt(account.getCredential()), (cursor, mail) -> {
                    int inserted = transaction.execute(tx -> {
                        int saved = 0;
                        if (mail != null) {
                            var duplicate = Wrappers.<MailMessage>lambdaQuery().eq(MailMessage::getAccountId, 1)
                                    .and(q -> q.eq(MailMessage::getFingerprint, mail.getFingerprint()).or()
                                            .eq(MailMessage::getUidValidity, cursor.validity()).eq(MailMessage::getUid, cursor.uid()));
                            if (messages.selectCount(duplicate) == 0) {
                                mail.setAccountId(1L); mail.setUidValidity(cursor.validity()); mail.setUid(cursor.uid());
                                messages.insert(mail); saved = 1;
                            }
                        }
                        checkpoint(cursor);
                        return saved;
                    });
                    counts[0] += inserted; counts[1]++;
                });
                transaction.executeWithoutResult(tx -> checkpoint(scan.cursor()));
                result = new MailOperation("SUCCESS", counts[0], counts[1], scan.hasMore(), null,
                        scan.hasMore() ? "本批同步完成，还有邮件待采集，请再次同步" : "同步完成");
            } catch (MailFailure failure) {
                result = failure(counts[1] > 0 ? "PARTIAL" : "FAILED", counts[0], counts[1], failure.kind());
            } catch (DataAccessException failure) {
                result = failure(counts[1] > 0 ? "PARTIAL" : "FAILED", counts[0], counts[1], MailFailure.Kind.STORAGE);
            }
            MailOperation completed = result;
            try {
                transaction.executeWithoutResult(tx -> {
                    var update = Wrappers.<MailboxAccount>lambdaUpdate().eq(MailboxAccount::getId, 1)
                            .set(MailboxAccount::getLastStatus, completed.status())
                            .set(MailboxAccount::getLastImported, completed.imported())
                            .set(MailboxAccount::getLastError, completed.errorCode());
                    if (completed.status().equals("SUCCESS")) update.set(MailboxAccount::getLastSuccessAt, clock.instant());
                    accounts.update(null, update);
                });
            } catch (DataAccessException failure) {
                return failure(counts[1] > 0 ? "PARTIAL" : "FAILED", counts[0], counts[1], MailFailure.Kind.STORAGE);
            }
            return result;
        });
    }

    private void checkpoint(MailClient.Cursor cursor) {
        if (accounts.update(null, Wrappers.<MailboxAccount>lambdaUpdate().eq(MailboxAccount::getId, 1)
                .set(MailboxAccount::getUidValidity, cursor.validity()).set(MailboxAccount::getLastUid, cursor.uid())) != 1)
            throw new BusinessException(ApiErrorCode.CONFLICT, "邮箱配置发生变化，请刷新后同步");
    }

    public PageResponse<MailSummary> list(String q, int page, int size) {
        var filter = Wrappers.<MailMessage>query().eq("account_id", 1);
        String search = InputText.search(q);
        if (search != null) filter.and(w -> w.apply("subject LIKE CONCAT('%', {0}, '%') ESCAPE '!'", search)
                .or().apply("sender LIKE CONCAT('%', {0}, '%') ESCAPE '!'", search));
        long count = messages.selectCount(filter);
        filter.select("id", "subject", "sender", "received_at", "sent_at", "content_status", "body_truncated")
                .orderByDesc("id").last("LIMIT " + size + " OFFSET " + (long) (page - 1) * size);
        return new PageResponse<>(messages.selectList(filter).stream().map(MailSummary::from).toList(), count, page, size);
    }

    public MailDetail detail(long id) {
        MailMessage m = messages.selectById(id);
        if (m == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "采集邮件不存在");
        return new MailDetail(MailSummary.from(m), m.getBodyText());
    }

    private MailboxAccount require() {
        MailboxAccount a = accounts.selectById(1);
        if (a == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "请先保存邮箱设置");
        return a;
    }
    private MailOperation failure(String status, int imported, int scanned, MailFailure.Kind kind) {
        return new MailOperation(status, imported, scanned, true, kind.name(), kind.message());
    }
    private <T> T exclusively(Supplier<T> action) {
        if (!gate.tryLock()) throw new BusinessException(ApiErrorCode.CONFLICT, "邮箱操作正在进行，请稍后重试");
        try { return action.get(); } finally { gate.unlock(); }
    }
}
