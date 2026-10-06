package io.github.ysuestc.offerflow.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ApiErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "请求格式不正确"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "不支持请求的响应格式"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请求内容类型不支持"),
    CONFLICT(HttpStatus.CONFLICT, "操作与当前资源状态冲突"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用，请稍后重试");

    private final HttpStatus status;
    private final String message;

    ApiErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public static ApiErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> BAD_REQUEST;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
        };
    }
}
