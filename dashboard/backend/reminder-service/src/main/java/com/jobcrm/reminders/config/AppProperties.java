package com.jobcrm.reminders.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed, validated application settings (prefix {@code app}).
 *
 * <p>Invalid values make the application fail at startup instead of misbehaving later.
 * Environment variables map automatically, e.g. {@code APP_CORS_ALLOWED_ORIGINS}
 * binds to {@code app.cors.allowed-origins}.
 *
 * <p>Later parts add their own groups here (database, mail, JWT) as they need them.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid @DefaultValue Cors cors) {

    /** An origin is scheme://host[:port] with no path and no trailing slash. */
    public static final String ORIGIN_REGEX = "^(https?|chrome-extension)://[^/\\s]+$";

    /**
     * Origins allowed to call the API from a browser. Consumed by the CORS configuration
     * added in Part 6. Empty by default, which means no cross-origin access.
     */
    public record Cors(
            List<@Pattern(
                    regexp = AppProperties.ORIGIN_REGEX,
                    message = "must look like https://host[:port] or chrome-extension://<id>, with no path")
                    String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }
}
