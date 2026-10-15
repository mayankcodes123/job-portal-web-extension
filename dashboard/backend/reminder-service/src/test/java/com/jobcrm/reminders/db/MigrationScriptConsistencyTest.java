package com.jobcrm.reminders.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcrm.reminders.reminder.Reminder;
import com.jobcrm.reminders.reminder.ReminderStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Checks the migration files WITHOUT a database. This catches naming mistakes and drift between
 * the SQL and the Java model, but it does not prove the SQL runs: that needs the
 * *IntegrationTest classes against a real PostgreSQL (see README).
 */
class MigrationScriptConsistencyTest {

    private static final Pattern FILE_NAME = Pattern.compile("V(\\d+)__[a-z0-9_]+\\.sql");

    private static List<Resource> migrations() throws IOException {
        Resource[] found = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
        return List.of(found);
    }

    /** The V1 script with every run of whitespace collapsed to one space. */
    private static String initialMigration() throws IOException {
        for (Resource resource : migrations()) {
            if ("V1__create_users_and_reminders.sql".equals(resource.getFilename())) {
                String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return raw.replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError("V1__create_users_and_reminders.sql not found on the classpath");
    }

    @Test
    void migrationFilesFollowFlywayNamingAndHaveUniqueVersions() throws IOException {
        List<Resource> files = migrations();
        assertThat(files).isNotEmpty();

        Set<String> versions = new HashSet<>();
        List<String> names = new ArrayList<>();
        for (Resource file : files) {
            String name = file.getFilename();
            names.add(name);
            Matcher matcher = FILE_NAME.matcher(name == null ? "" : name);
            assertThat(matcher.matches()).as("file name %s", name).isTrue();
            assertThat(versions.add(matcher.group(1))).as("duplicate version in %s", names).isTrue();
        }
    }

    @Test
    void statusConstraintListsExactlyTheJavaEnumValues() throws IOException {
        String sql = initialMigration();
        Matcher matcher = Pattern.compile("status IN \\(([^)]*)\\)").matcher(sql);
        assertThat(matcher.find()).as("status CHECK constraint present").isTrue();

        Set<String> inDatabase = new HashSet<>();
        for (String token : matcher.group(1).split(",")) {
            inDatabase.add(token.trim().replace("'", ""));
        }

        Set<String> inJava = new HashSet<>();
        for (ReminderStatus status : ReminderStatus.values()) {
            inJava.add(status.name());
        }

        assertThat(inDatabase).isEqualTo(inJava);
    }

    @Test
    void schemaMatchesTheDocumentedContractRules() throws IOException {
        String sql = initialMigration();

        assertThat(sql).contains("UNIQUE (user_id, client_job_id)");
        assertThat(sql).contains("REFERENCES users (id) ON DELETE CASCADE");
        assertThat(sql).contains("timezone VARCHAR(64) NOT NULL DEFAULT '" + Reminder.DEFAULT_TIMEZONE + "'");
        assertThat(sql).contains("send_time TIME NOT NULL DEFAULT TIME '09:00'");
        assertThat(sql).contains("remind_at TIMESTAMPTZ NOT NULL");
        assertThat(sql).contains("CREATE INDEX ix_reminders_due_pending ON reminders (remind_at) WHERE status = 'PENDING'");
    }

    @Test
    void columnLengthsMatchTheJavaLimits() throws IOException {
        String sql = initialMigration();

        assertThat(sql).contains("job_title VARCHAR(" + Reminder.TITLE_MAX_LENGTH + ")");
        assertThat(sql).contains("company VARCHAR(" + Reminder.COMPANY_MAX_LENGTH + ")");
        assertThat(sql).contains("job_url VARCHAR(" + Reminder.URL_MAX_LENGTH + ")");
    }
}
