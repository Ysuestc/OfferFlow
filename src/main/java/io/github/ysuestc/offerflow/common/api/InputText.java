package io.github.ysuestc.offerflow.common.api;

import io.github.ysuestc.offerflow.common.exception.BusinessException;
import java.net.URI;

public final class InputText {
    private InputText() { }

    public static String optional(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    public static String search(String text) {
        String value = optional(text);
        return value == null ? null : value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    public static String website(String text) {
        String value = optional(text);
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) return value;
        } catch (IllegalArgumentException ignored) {
            // Invalid user input is translated to a controlled business error below.
        }
        throw new BusinessException(ApiErrorCode.BAD_REQUEST, "官网须填写有效的 http 或 https 地址");
    }
}
