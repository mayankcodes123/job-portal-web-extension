package com.jobcrm.reminders.db;

import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for tests that need a real PostgreSQL. Subclasses must carry
 * {@code @EnabledIfEnvironmentVariable(named = URL_ENV, matches = ".+")} so they are SKIPPED
 * (not failed, not faked) when no test database is configured.
 *
 * <p>Safety: the first test class wipes the schema with Flyway's clean before the application
 * starts, and every test truncates the tables. To make that harmless, the database name in the
 * JDBC URL MUST end in "_test"; anything else aborts the run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractPostgresIntegrationTest {

    public static final String URL_ENV = "REMINDER_TEST_DB_URL";
    public static final String USER_ENV = "REMINDER_TEST_DB_USERNAME";
    public static final String PASSWORD_ENV = "REMINDER_TEST_DB_PASSWORD";

    private static final AtomicBoolean DATABASE_PREPARED = new AtomicBoolean(false);

    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    protected static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnv(URL_ENV));
        registry.add("spring.datasource.username", () -> requiredEnv(USER_ENV));
        registry.add("spring.datasource.password", () -> requiredEnv(PASSWORD_ENV));
    }

    @BeforeAll
    protected static void prepareDatabaseOnce() {
        if (DATABASE_PREPARED.get()) {
            return;
        }
        String url = requiredEnv(URL_ENV);
        assertDedicatedTestDatabase(url);

        // Start from an empty schema so the migrations are tested from scratch on every run.
        Flyway.configure()
                .dataSource(url, requiredEnv(USER_ENV), requiredEnv(PASSWORD_ENV))
                .cleanDisabled(false)
                .locations("classpath:db/migration")
                .load()
                .clean();

        DATABASE_PREPARED.set(true);
    }

    @BeforeEach
    protected void emptyTables() {
        jdbc.execute("TRUNCATE TABLE reminders, users CASCADE");
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable " + name + " must be set to run the database integration tests.");
        }
        return value;
    }

    private static void assertDedicatedTestDatabase(String url) {
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException(URL_ENV + " must be a jdbc:postgresql: URL.");
        }
        String withoutQuery = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String databaseName = withoutQuery.substring(withoutQuery.lastIndexOf('/') + 1);
        if (!databaseName.endsWith("_test")) {
            throw new IllegalStateException(
                    "Refusing to run: the integration tests wipe the database, so its name must end with "
                            + "'_test' (for example reminders_test). Got: '" + databaseName + "'.");
        }
    }
}
