package com.jobcrm.reminders.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Error envelope shared by every endpoint:
 * {@code {"error": {"code": "...", "message": "...", "fields": {...}}}}.
 * {@code fields} is omitted when empty.
 */
public record ApiError(Body error) {

    public record Body(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> fields) {}

    public static ApiError of(String code, String message) {
        return new ApiError(new Body(code, message, null));
    }

    public static ApiError of(String code, String message, Map<String, String> fields) {
        return new ApiError(new Body(code, message, fields));
    }
}
