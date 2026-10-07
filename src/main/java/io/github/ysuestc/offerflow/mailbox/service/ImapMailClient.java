package io.github.ysuestc.offerflow.mailbox.service;

import io.github.ysuestc.offerflow.mailbox.entity.*;
import jakarta.mail.*;
import jakarta.mail.search.*;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("mysql")
public class ImapMailClient implements MailClient {
    protected String host(MailboxAccount account) { return account.getProvider().host(); }
    protected int port() { return 993; }
    protected Properties properties() {
        Properties p = new Properties();
        p.setProperty("mail.imaps.ssl.enable", "true");
        p.setProperty("mail.imaps.ssl.checkserveridentity", "true");
        p.setProperty("mail.imaps.connectiontimeout", "10000");
        p.setProperty("mail.imaps.timeout", "15000");
        p.setProperty("mail.imaps.writetimeout", "15000");
        p.setProperty("mail.imaps.peek", "true");
        return p;
    }

    @Override public void test(MailboxAccount account, String code) {
        try (Store store = connect(account, code)) {
            Folder folder = open(store, account);
            folder.close(false);
        } catch (AuthenticationFailedException e) { throw new MailFailure(MailFailure.Kind.AUTHENTICATION); }
        catch (MessagingException e) { throw new MailFailure(MailFailure.Kind.NETWORK); }
    }

    @Override public ScanResult scan(MailboxAccount account, String code, BiConsumer<Cursor, MailMessage> persist) {
        try (Store store = connect(account, code)) {
            Folder folder = open(store, account);
            try {
                UIDFolder uidFolder = (UIDFolder) folder;
                long validity = uidFolder.getUIDValidity();
                if (validity <= 0) throw new MailFailure(MailFailure.Kind.NETWORK);
                // Capture before searching: mail delivered during body downloads must not be skipped.
                long snapshotNext = uidFolder.getUIDNext();
                long previous = Objects.equals(account.getUidValidity(), validity) && account.getLastUid() != null
                        ? account.getLastUid() : 0;
                Message[] candidates = previous == 0
                        ? folder.search(new ReceivedDateTerm(ComparisonTerm.GE,
                            Date.from(account.getSyncFrom().truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS))))
                        : uidFolder.getMessagesByUID(previous + 1, UIDFolder.LASTUID);
                FetchProfile profile = new FetchProfile();
                profile.add(UIDFolder.FetchProfileItem.UID);
                folder.fetch(candidates, profile);
                var ordered = new TreeMap<Long, Message>();
                for (Message m : candidates) {
                    long uid = uidFolder.getUID(m);
                    if (uid > previous) ordered.put(uid, m);
                }
                int scanned = 0;
                long cursor = previous;
                MailContentReader reader = new MailContentReader();
                for (var entry : ordered.entrySet()) {
                    if (scanned == 100) break;
                    Message m = entry.getValue();
                    if (m.getReceivedDate() == null) throw new MailFailure(MailFailure.Kind.CONTENT);
                    MailMessage mail = m.getReceivedDate().toInstant().isBefore(account.getSyncFrom()) ? null : reader.read(m);
                    cursor = entry.getKey();
                    persist.accept(new Cursor(validity, cursor), mail);
                    scanned++;
                }
                boolean hasMore = ordered.size() > scanned;
                if (!hasMore) cursor = Math.max(cursor, snapshotNext - 1);
                return new ScanResult(new Cursor(validity, Math.max(0, cursor)), hasMore);
            } finally { folder.close(false); }
        } catch (AuthenticationFailedException e) { throw new MailFailure(MailFailure.Kind.AUTHENTICATION); }
        catch (IOException e) { throw new MailFailure(MailFailure.Kind.CONTENT); }
        catch (MessagingException e) { throw new MailFailure(MailFailure.Kind.NETWORK); }
    }

    private Store connect(MailboxAccount account, String code) throws MessagingException {
        Session session = Session.getInstance(properties());
        session.setDebug(false);
        Store store = session.getStore("imaps");
        try {
            store.connect(host(account), port(), account.getEmail(), code);
            if (store instanceof IMAPStore imap && imap.hasCapability("ID"))
                imap.id(Map.of("name", "OfferFlow", "version", "0.1"));
            return store;
        } catch (MessagingException e) {
            try { store.close(); } catch (MessagingException cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }

    private Folder open(Store store, MailboxAccount account) throws MessagingException {
        Folder folder = store.getFolder(account.getFolder());
        if (!folder.exists()) throw new MailFailure(MailFailure.Kind.FOLDER);
        try { folder.open(Folder.READ_ONLY); }
        catch (MessagingException e) { throw new MailFailure(MailFailure.Kind.FOLDER); }
        return folder;
    }
}
