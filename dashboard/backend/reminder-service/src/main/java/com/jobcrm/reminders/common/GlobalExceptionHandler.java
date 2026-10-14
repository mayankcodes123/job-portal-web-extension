package com.jobcrm.reminders.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into the {@link ApiError} envelope. Spring MVC's own exceptions
 * (bad JSON, wrong method, unknown path, ...) go through {@link #handleExceptionInternal};
 * anything unexpected becomes a generic 500 and is logged, never echoed to the client.
 *
 * <p>Domain exceptions (e.g. FOLLOW_UP_IN_PAST) are added here in later parts.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ApiError apiError;

        if (ex instanceof MethodArgumentNotValidException invalid) {
            Map<String, String> fields = new LinkedHashMap<>();
            for (FieldError fieldError : invalid.getBindingResult().getFieldErrors()) {
                String message = fieldError.getDefaultMessage();
                fields.putIfAbsent(fieldError.getField(), message == null ? "invalid value" : message);
            }
            apiError = ApiError.of("VALIDATION_ERROR", "Request validation failed.", fields);
        } else if (ex instanceof HttpMessageNotReadableException) {
            apiError = ApiError.of("MALFORMED_REQUEST", "Request body is missing or malformed.");
        } else {
            apiError = fromStatus(statusCode);
        }

        return ResponseEntity.status(statusCode).headers(headers).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }

    private static ApiError fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ApiError.of("BAD_REQUEST", "The request could not be processed.");
            case 404 -> ApiError.of("NOT_FOUND", "Resource not found.");
            case 405 -> ApiError.of("METHOD_NOT_ALLOWED", "HTTP method not allowed for this resource.");
            case 406 -> ApiError.of("NOT_ACCEPTABLE", "Requested response format is not supported.");
            case 415 -> ApiError.of("UNSUPPORTED_MEDIA_TYPE", "Content type is not supported.");
            default -> status.is5xxServerError()
                    ? ApiError.of("INTERNAL_ERROR", "An unexpected error occurred.")
                    : ApiError.of("REQUEST_ERROR", "The request could not be processed.");
        };
    }
}
