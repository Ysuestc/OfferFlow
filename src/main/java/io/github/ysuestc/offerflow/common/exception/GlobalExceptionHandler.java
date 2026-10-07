package io.github.ysuestc.offerflow.common.exception;

import java.util.Comparator;
import java.sql.SQLException;

import io.github.ysuestc.offerflow.common.api.ApiErrorCode;
import io.github.ysuestc.offerflow.common.api.ApiResponse;
import io.github.ysuestc.offerflow.common.api.FieldViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ApiErrorCode code = exception.errorCode();
        if (code.status().is5xxServerError()) {
            log.error("Business operation failed", exception);
            return ResponseEntity.status(code.status()).body(ApiResponse.failure(code));
        }
        return ResponseEntity.status(code.status())
                .body(ApiResponse.failure(code, exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrityException(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof SQLException sql) {
            String message = switch (sql.getErrorCode()) {
                case 1062 -> "该岗位已有投递档案，请打开已有档案";
                case 1451 -> "该记录已被引用，请先处理关联记录";
                case 1452 -> "关联记录已发生变化，请刷新后重试";
                default -> null;
            };
            if (message != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.failure(ApiErrorCode.CONFLICT, message));
            }
        }
        return handleUnexpectedException(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unexpected request failure", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.failure(ApiErrorCode.INTERNAL_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(),
                        error.isBindingFailure() ? "值的格式不正确"
                                : error.getDefaultMessage() == null ? "值不合法" : error.getDefaultMessage()))
                .distinct()
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();
        return handleExceptionInternal(exception, ApiResponse.validationFailure(violations),
                headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        // No JSON response can satisfy a client that explicitly excludes JSON.
        return new ResponseEntity<>(null, headers, status);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Object response = body;
        if (!(body instanceof ApiResponse<?>)) {
            ApiErrorCode code = ApiErrorCode.fromStatus(status);
            if (exception instanceof HandlerMethodValidationException && status.is4xxClientError()) {
                code = ApiErrorCode.VALIDATION_ERROR;
            }
            response = ApiResponse.failure(code);
        }
        if (status.is5xxServerError()) {
            log.error("Request failed with HTTP {}", status.value(), exception);
        }
        return super.handleExceptionInternal(exception, response, headers, status, request);
    }
}
