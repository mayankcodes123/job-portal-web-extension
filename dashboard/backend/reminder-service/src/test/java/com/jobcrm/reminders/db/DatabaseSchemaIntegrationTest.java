package com.jobcrm.reminders.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;

/** Exercises the Flyway-created schema directly with SQL. Needs PostgreSQL; see README. */
@EnabledIfEnvironmentVariable(named = AbstractPostgresIntegrationTest.URL_ENV, matches = ".+")
class DatabaseSchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", id, email);
        return id;
    }

    private void insertReminder(UUID userId, UUID clientJobId, String title, String company, String status) {
        jdbc.update(
                """
                INSERT INTO reminders
                    (user_id, client_job_id, job_title, company, follow_up_date, remind_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                clientJobId,
                title,
                company,
                Date.valueOf(LocalDate.of(2026, 10, 15)),
                Timestamp.from(Instant.parse("2026-10-15T03:30:00Z")),
                status);
    }

    private void insertSentReminder(UUID userId, UUID clientJobId) {
        jdbc.update(
                """
                INSERT INTO reminders
                    (user_id, client_job_id, job_title, company, follow_up_date, remind_at, status, sent_at)
                VALUES (?, ?, 'Engineer', 'ABC', ?, ?, 'SENT', now())
                """,
                userId,
                clientJobId,
                Date.valueOf(LocalDate.of(2026, 10, 15)),
                Timestamp.from(Instant.parse("2026-10-15T03:30:00Z")));
    }

    private long countReminders() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM reminders", Long.class);
        return count == null ? 0 : count;
    }

    @Test
    void flywayRecordedTheInitialMigration() {
        Long applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success", Long.class);
        assertThat(applied).isEqualTo(1L);
    }

    @Test
    void bothTablesExist() {
        Long tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN ('users', 'reminders')",
                Long.class);
        assertThat(tables).isEqualTo(2L);
    }

    @Test
    void omittedColumnsGetTheDocumentedDefaults() {
        UUID userId = insertUser("candidate@example.com");
        jdbc.update(
                """
                INSERT INTO reminders (user_id, client_job_id, job_title, company, follow_up_date, remind_at)
                VALUES (?, ?, 'Engineer', 'ABC', ?, ?)
                """,
                userId,
                UUID.randomUUID(),
                Date.valueOf(LocalDate.of(2026, 10, 15)),
                Timestamp.from(Instant.parse("2026-10-15T03:30:00Z")));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT timezone, send_time::text AS send_time, status, "
                        + "(id IS NOT NULL) AS has_id, (created_at IS NOT NULL) AS has_created "
                        + "FROM reminders WHERE user_id = ?",
                userId);

        assertThat(row.get("timezone")).isEqualTo("Asia/Kolkata");
        assertThat(row.get("send_time")).isEqualTo("09:00:00");
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("has_id")).isEqualTo(true);
        assertThat(row.get("has_created")).isEqualTo(true);
    }

    @Test
    void oneReminderPerUserAndClientJob() {
        UUID userA = insertUser("a@example.com");
        UUID userB = insertUser("b@example.com");
        UUID clientJobId = UUID.randomUUID();

        insertReminder(userA, clientJobId, "Engineer", "ABC", "PENDING");

        assertThatThrownBy(() -> insertReminder(userA, clientJobId, "Engineer", "ABC", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);

        // The same job id for a different user is a different reminder.
        insertReminder(userB, clientJobId, "Engineer", "ABC", "PENDING");
        assertThat(countReminders()).isEqualTo(2L);
    }

    @Test
    void everyDeclaredStatusIsAccepted() {
        UUID userId = insertUser("candidate@example.com");

        for (String status : List.of("PENDING", "PROCESSING", "FAILED", "CANCELLED")) {
            insertReminder(userId, UUID.randomUUID(), "Engineer", "ABC", status);
        }
        insertSentReminder(userId, UUID.randomUUID());

        assertThat(countReminders()).isEqualTo(5L);
    }

    @Test
    void unknownStatusIsRejected() {
        UUID userId = insertUser("candidate@example.com");

        assertThatThrownBy(() -> insertReminder(userId, UUID.randomUUID(), "Engineer", "ABC", "BOGUS"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertReminder(userId, UUID.randomUUID(), "Engineer", "ABC", "pending"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sentStatusRequiresASentTimestamp() {
        UUID userId = insertUser("candidate@example.com");

        assertThatThrownBy(() -> insertReminder(userId, UUID.randomUUID(), "Engineer", "ABC", "SENT"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void blankTitleOrCompanyIsRejected() {
        UUID userId = insertUser("candidate@example.com");

        assertThatThrownBy(() -> insertReminder(userId, UUID.randomUUID(), "   ", "ABC", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertReminder(userId, UUID.randomUUID(), "Engineer", "", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reminderMustBelongToAnExistingUser() {
        assertThatThrownBy(() -> insertReminder(UUID.randomUUID(), UUID.randomUUID(), "Engineer", "ABC", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAUserDeletesTheirReminders() {
        UUID userId = insertUser("candidate@example.com");
        insertReminder(userId, UUID.randomUUID(), "Engineer", "ABC", "PENDING");
        insertReminder(userId, UUID.randomUUID(), "Analyst", "XYZ", "PENDING");
        assertThat(countReminders()).isEqualTo(2L);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        assertThat(countReminders()).isZero();
    }

    @Test
    void emailsMustBeUniqueAndNormalized() {
        insertUser("candidate@example.com");

        assertThatThrownBy(() -> insertUser("candidate@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertUser("Candidate2@Example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertUser(" padded@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void partialIndexForDueRemindersExists() {
        Long indexes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'reminders' "
                        + "AND indexname = 'ix_reminders_due_pending' AND indexdef LIKE '%PENDING%'",
                Long.class);
        assertThat(indexes).isEqualTo(1L);
    }
}
