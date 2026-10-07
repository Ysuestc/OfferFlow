package io.github.ysuestc.offerflow.mailbox.controller;

import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.mailbox.dto.MailboxRequest;
import io.github.ysuestc.offerflow.mailbox.service.MailboxService;
import io.github.ysuestc.offerflow.mailbox.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController @Profile("mysql") @RequiredArgsConstructor
@RequestMapping("/api/v1/mailbox")
public class MailboxController {
    private final MailboxService service;

    @ModelAttribute
    void preventCaching(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
    @GetMapping public ApiResponse<MailboxView> settings() { return safe(service::get); }
    @PutMapping public ApiResponse<MailboxView> save(@Valid @RequestBody MailboxRequest request) {
        return safe(() -> service.save(request));
    }
    @PostMapping("/connection-tests") public ApiResponse<MailOperation> test() { return safe(service::testConnection); }
    @PostMapping("/syncs") public ApiResponse<MailOperation> sync() { return safe(service::sync); }
    @GetMapping("/messages") public ApiResponse<PageResponse<MailSummary>> list(
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return safe(() -> service.list(q, page, size));
    }
    @GetMapping("/messages/{id}") public ApiResponse<MailDetail> detail(@PathVariable @Positive long id) {
        return safe(() -> service.detail(id));
    }
    private <T> ApiResponse<T> safe(Supplier<T> action) {
        try { return ApiResponse.success(action.get()); }
        catch (DataAccessException e) {
            // Do not pass SQL exceptions with mail bodies or encrypted credentials into shared logging.
            throw new BusinessException(ApiErrorCode.INTERNAL_ERROR);
        }
    }
}
