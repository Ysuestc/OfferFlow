package io.github.ysuestc.offerflow.mailbox.service;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CredentialVaultTest {
    @TempDir Path directory;
    @Test void encryptsWithFreshNonceAndSurvivesRestart() {
        var vault = new CredentialVault(directory.toString());
        byte[] a = vault.encrypt("DemoAuthCode12345");
        byte[] b = vault.encrypt("DemoAuthCode12345");
        assertThat(a).isNotEqualTo(b);
        assertThat(new String(a, java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("DemoAuthCode12345");
        assertThat(new CredentialVault(directory.toString()).decrypt(a)).isEqualTo("DemoAuthCode12345");
    }
    @Test void missingKeyDoesNotGenerateReplacementDuringRead() throws Exception {
        var vault = new CredentialVault(directory.toString());
        byte[] encrypted = vault.encrypt("DemoAuthCode12345");
        Files.delete(directory.resolve("mail.key"));
        assertThatThrownBy(() -> vault.decrypt(encrypted)).isInstanceOf(MailFailure.class);
        assertThat(directory.resolve("mail.key")).doesNotExist();
        assertThat(vault.decrypt(vault.encrypt("NewDemoAuth123456"))).isEqualTo("NewDemoAuth123456");
    }
    @Test void rejectsCorruptionAndDoesNotExposePayload() throws Exception {
        var vault = new CredentialVault(directory.toString());
        byte[] encrypted = vault.encrypt("DemoAuthCode12345");
        encrypted[encrypted.length - 1] ^= 1;
        assertThatThrownBy(() -> vault.decrypt(encrypted)).isInstanceOf(MailFailure.class)
                .hasMessage(MailFailure.Kind.CREDENTIAL.message()).hasNoCause();
        Files.write(directory.resolve("mail.key"), new byte[2]);
        assertThatThrownBy(() -> vault.encrypt("AnotherDemo12345")).isInstanceOf(MailFailure.class);
    }
}
