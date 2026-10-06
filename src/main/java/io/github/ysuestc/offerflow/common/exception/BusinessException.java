package io.github.ysuestc.offerflow.common.exception;

import java.util.Objects;

import io.github.ysuestc.offerflow.common.api.ApiErrorCode;

public final class BusinessException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public BusinessException(ApiErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
    }

    public ApiErrorCode errorCode() {
        return errorCode;
    }
}
