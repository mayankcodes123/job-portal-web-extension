package com.jobcrm.reminders.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobcrm.reminders.db.AbstractPostgresIntegrationTest;
import com.jobcrm.reminders.user.User;
import com.jobcrm.reminders.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Round-trips the JPA entities through a real PostgreSQL. Starting the context also proves
 * Hibernate's schema validation accepts the Flyway-created tables. Needs PostgreSQL; see README.
 */
@EnabledIfEnvironmentVariable(named = AbstractPostgresIntegrationTest.URL_ENV, matches = ".+")
class ReminderPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    private UserRepository users;

    @Autowired
    private ReminderRepository reminders;

    private static Reminder reminderFor(UUID userId, UUID clientJobId, LocalDate followUpDate) {
        return new Reminder(
                userId,
                clientJobId,
                "Software Engineer",
                "ABC Company",
                "https://www.naukri.com/job-listings-abc-123",
                followUpDate,
                IST,
                LocalTime.of(9, 0));
    }

    @Test
    void userIsSavedWithANormalizedEmailAndFoundByIt() {
        User saved = users.saveAndFlush(new User("  Candidate@Example.COM "));

        assertThat(saved.getId()).isNotNull();
        User loaded = users.findByEmail("candidate@example.com").orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getVerifiedAt()).isNull();
    }

    @Test
    void duplicateEmailIsRejectedByTheDatabase() {
        users.saveAndFlush(new User("candidate@example.com"));

        assertThatThrownBy(() -> users.saveAndFlush(new User("CANDIDATE@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reminderRoundTripsWithAServerComputedRemindAt() {
        User user = users.saveAndFlush(new User("candidate@example.com"));
        UUID clientJobId = UUID.randomUUID();

        Reminder saved = reminders.saveAndFlush(reminderFor(user.getId(), clientJobId, LocalDate.of(2026, 10, 15)));
        Reminder loaded = reminders.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getUserId()).isEqualTo(user.getId());
        assertThat(loaded.getClientJobId()).isEqualTo(clientJobId);
        assertThat(loaded.getJobTitle()).isEqualTo("Software Engineer");
        assertThat(loaded.getCompany()).isEqualTo("ABC Company");
        assertThat(loaded.getJobUrl()).isEqualTo("https://www.naukri.com/job-listings-abc-123");
        assertThat(loaded.getFollowUpDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(loaded.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(loaded.getSendTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(loaded.getRemindAt()).isEqualTo(Instant.parse("2026-10-15T03:30:00Z"));
        assertThat(loaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(loaded.getSentAt()).isNull();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void jobUrlIsOptional() {
        User user = users.saveAndFlush(new User("candidate@example.com"));
        Reminder noUrl = new Reminder(
                user.getId(), UUID.randomUUID(), "Engineer", "ABC", null,
                LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0));

        Reminder loaded = reminders.findById(reminders.saveAndFlush(noUrl).getId()).orElseThrow();

        assertThat(loaded.getJobUrl()).isNull();
    }

    @Test
    void secondReminderForTheSameUserAndJobViolatesTheUniqueConstraint() {
        User user = users.saveAndFlush(new User("candidate@example.com"));
        UUID clientJobId = UUID.randomUUID();
        reminders.saveAndFlush(reminderFor(user.getId(), clientJobId, LocalDate.of(2026, 10, 15)));

        assertThatThrownBy(() ->
                        reminders.saveAndFlush(reminderFor(user.getId(), clientJobId, LocalDate.of(2026, 10, 16))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsByUserAndClientJobIdOnlyForTheOwner() {
        User owner = users.saveAndFlush(new User("owner@example.com"));
        User other = users.saveAndFlush(new User("other@example.com"));
        UUID clientJobId = UUID.randomUUID();
        reminders.saveAndFlush(reminderFor(owner.getId(), clientJobId, LocalDate.of(2026, 10, 15)));

        assertThat(reminders.findByUserIdAndClientJobId(owner.getId(), clientJobId)).isPresent();
        assertThat(reminders.findByUserIdAndClientJobId(other.getId(), clientJobId)).isEmpty();
        assertThat(reminders.findByUserIdAndClientJobId(owner.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void listsOnlyTheUsersOwnRemindersEarliestFirst() {
        User owner = users.saveAndFlush(new User("owner@example.com"));
        User other = users.saveAndFlush(new User("other@example.com"));
        reminders.saveAndFlush(reminderFor(owner.getId(), UUID.randomUUID(), LocalDate.of(2026, 10, 20)));
        reminders.saveAndFlush(reminderFor(owner.getId(), UUID.randomUUID(), LocalDate.of(2026, 10, 15)));
        reminders.saveAndFlush(reminderFor(other.getId(), UUID.randomUUID(), LocalDate.of(2026, 10, 1)));

        List<Reminder> mine = reminders.findByUserIdOrderByRemindAtAsc(owner.getId());

        assertThat(mine).extracting(Reminder::getFollowUpDate)
                .containsExactly(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20));
    }

    @Test
    void reschedulingASentReminderIsPersistedAndRearmsIt() {
        User user = users.saveAndFlush(new User("candidate@example.com"));
        Reminder created = reminders.saveAndFlush(
                reminderFor(user.getId(), UUID.randomUUID(), LocalDate.of(2026, 10, 15)));

        Reminder sent = reminders.findById(created.getId()).orElseThrow();
        sent.markSent(Instant.parse("2026-10-15T03:31:00Z"));
        reminders.saveAndFlush(sent);

        Reminder reloadedSent = reminders.findById(created.getId()).orElseThrow();
        assertThat(reloadedSent.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(reloadedSent.getSentAt()).isEqualTo(Instant.parse("2026-10-15T03:31:00Z"));

        reloadedSent.reschedule(LocalDate.of(2026, 10, 20), IST, LocalTime.of(10, 30));
        reminders.saveAndFlush(reloadedSent);

        Reminder rearmed = reminders.findById(created.getId()).orElseThrow();
        assertThat(rearmed.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(rearmed.getSentAt()).isNull();
        assertThat(rearmed.getFollowUpDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(rearmed.getRemindAt()).isEqualTo(Instant.parse("2026-10-20T05:00:00Z"));
        assertThat(rearmed.getUpdatedAt()).isAfterOrEqualTo(rearmed.getCreatedAt());
    }
}
