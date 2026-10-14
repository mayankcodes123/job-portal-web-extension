package com.jobcrm.reminders.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    static class PropertiesConfig {}

    @Test
    void allowedOriginsDefaultToEmpty() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AppProperties.class).cors().allowedOrigins()).isEmpty();
        });
    }

    @Test
    void bindsCommaSeparatedOrigins() {
        runner.withPropertyValues(
                        "app.cors.allowed-origins=http://localhost:5173,chrome-extension://abcdefghijklmnopabcdefghijklmnop")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AppProperties.class).cors().allowedOrigins())
                            .containsExactly(
                                    "http://localhost:5173",
                                    "chrome-extension://abcdefghijklmnopabcdefghijklmnop");
                });
    }

    @Test
    void rejectsOriginWithUnsupportedScheme() {
        runner.withPropertyValues("app.cors.allowed-origins=ftp://example.com")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsOriginWithTrailingSlash() {
        runner.withPropertyValues("app.cors.allowed-origins=https://example.com/")
                .run(context -> assertThat(context).hasFailed());
    }
}
