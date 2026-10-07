package io.github.ysuestc.offerflow.mailbox.dto;

import io.github.ysuestc.offerflow.mailbox.entity.MailProvider;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record MailboxRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotNull MailProvider provider, @NotBlank @Size(max = 128) String folder,
        @NotNull @PastOrPresent Instant syncFrom, @Size(max = 128) String authorizationCode,
        @NotNull @Min(0) Integer version) {
    @Override public String toString() { return "MailboxRequest"; }
}
