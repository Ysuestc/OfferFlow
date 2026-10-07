package io.github.ysuestc.offerflow.mailbox.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("mysql")
public class CredentialVault {
    private final Path keyFile;
    private final SecureRandom random = new SecureRandom();

    public CredentialVault(@Value("${offerflow.mailbox.private-dir:private-data/mailbox}") String directory) {
        keyFile = Path.of(directory).toAbsolutePath().normalize().resolve("mail.key");
    }

    public byte[] encrypt(String value) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(true), "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
        } catch (Exception e) { throw new MailFailure(MailFailure.Kind.CREDENTIAL); }
    }

    public String decrypt(byte[] encrypted) {
        try {
            ByteBuffer input = ByteBuffer.wrap(encrypted);
            byte[] nonce = new byte[12];
            input.get(nonce);
            byte[] payload = new byte[input.remaining()];
            input.get(payload);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(false), "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new MailFailure(MailFailure.Kind.CREDENTIAL); }
    }

    private synchronized byte[] key(boolean create) throws Exception {
        if (!Files.exists(keyFile) && create) {
            Files.createDirectories(keyFile.getParent());
            byte[] generated = new byte[32];
            random.nextBytes(generated);
            try {
                if (Files.getFileStore(keyFile.getParent()).supportsFileAttributeView("posix"))
                    Files.createFile(keyFile, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                else Files.createFile(keyFile);
                Files.write(keyFile, generated, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException e) { /* Another creator won; use its key. */ }
        }
        byte[] bytes = Files.readAllBytes(keyFile);
        if (bytes.length != 32) throw new IllegalStateException("Invalid local key");
        return bytes;
    }
}
