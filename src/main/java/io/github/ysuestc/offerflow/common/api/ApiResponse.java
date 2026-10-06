package io.github.ysuestc.offerflow.common.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldViolation> errors) {

    public ApiResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data, List.of());
    }

    public static ApiResponse<Void> failure(ApiErrorCode code) {
        return failure(code, code.message());
    }

    public static ApiResponse<Void> failure(ApiErrorCode code, String message) {
        return new ApiResponse<>(code.name(), message, null, List.of());
    }

    public static ApiResponse<Void> validationFailure(List<FieldViolation> errors) {
        return new ApiResponse<>(ApiErrorCode.VALIDATION_ERROR.name(),
                ApiErrorCode.VALIDATION_ERROR.message(), null, errors);
    }
}
