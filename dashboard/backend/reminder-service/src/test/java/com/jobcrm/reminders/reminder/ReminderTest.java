package com.jobcrm.reminders.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CLIENT_JOB_ID = UUID.randomUUID();
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static Reminder reminder() {
        return new Reminder(
                USER_ID,
                CLIENT_JOB_ID,
                "  Software Engineer ",
                "ABC Company",
                null,
                LocalDate.of(2026, 10, 15),
                IST,
                LocalTime.of(9, 0));
    }

    @Test
    void newReminderIsPendingAndKeepsItsIdentifiers() {
        Reminder reminder = reminder();

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reminder.getUserId()).isEqualTo(USER_ID);
        assertThat(reminder.getClientJobId()).isEqualTo(CLIENT_JOB_ID);
        assertThat(reminder.getSentAt()).isNull();
    }

    @Test
    void defaultsMatchTheContract() {
        assertThat(Reminder.DEFAULT_TIMEZONE).isEqualTo("Asia/Kolkata");
        assertThat(Reminder.DEFAULT_SEND_TIME).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void remindAtIsTheLocalDateAndTimeInTheUsersZone() {
        // 09:00 in India (UTC+05:30, no daylight saving) is 03:30 UTC.
        assertThat(reminder().getRemindAt()).isEqualTo(Instant.parse("2026-10-15T03:30:00Z"));
        assertThat(reminder().getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void remindAtRespectsDaylightSavingTime() {
        ZoneId newYork = ZoneId.of("America/New_York");

        assertThat(Reminder.computeRemindAt(LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), newYork))
                .isEqualTo(Instant.parse("2026-07-01T13:00:00Z")); // EDT, UTC-4
        assertThat(Reminder.computeRemindAt(LocalDate.of(2026, 12, 1), LocalTime.of(9, 0), newYork))
                .isEqualTo(Instant.parse("2026-12-01T14:00:00Z")); // EST, UTC-5
    }

    @Test
    void sendTimeIsKeptAtMinutePrecision() {
        Reminder reminder = new Reminder(
                USER_ID, CLIENT_JOB_ID, "Engineer", "ABC", null,
                LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 30, 45, 123));

        assertThat(reminder.getSendTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(reminder.getRemindAt()).isEqualTo(Instant.parse("2026-10-15T04:00:00Z"));
    }

    @Test
    void textIsTrimmedAndBlankUrlBecomesAbsent() {
        Reminder reminder = new Reminder(
                USER_ID, CLIENT_JOB_ID, "  Engineer  ", "  ABC  ", "   ",
                LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0));

        assertThat(reminder.getJobTitle()).isEqualTo("Engineer");
        assertThat(reminder.getCompany()).isEqualTo("ABC");
        assertThat(reminder.getJobUrl()).isNull();
    }

    @Test
    void rejectsBlankOrOversizedText() {
        assertThatThrownBy(() -> new Reminder(
                        USER_ID, CLIENT_JOB_ID, "   ", "ABC", null,
                        LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Reminder(
                        USER_ID, CLIENT_JOB_ID, "Engineer", "", null,
                        LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class);

        String tooLong = "x".repeat(Reminder.TITLE_MAX_LENGTH + 1);
        assertThatThrownBy(() -> new Reminder(
                        USER_ID, CLIENT_JOB_ID, tooLong, "ABC", null,
                        LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingRequiredValues() {
        assertThatThrownBy(() -> new Reminder(
                        null, CLIENT_JOB_ID, "Engineer", "ABC", null,
                        LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0)))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Reminder(
                        USER_ID, CLIENT_JOB_ID, "Engineer", "ABC", null,
                        null, IST, LocalTime.of(9, 0)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rescheduleRecomputesRemindAtAndRearmsASentReminder() {
        Reminder reminder = reminder();
        reminder.markSent(Instant.parse("2026-10-15T03:31:00Z"));
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(reminder.getSentAt()).isNotNull();

        reminder.reschedule(LocalDate.of(2026, 10, 20), IST, LocalTime.of(10, 30));

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reminder.getSentAt()).isNull();
        assertThat(reminder.getFollowUpDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(reminder.getSendTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(reminder.getRemindAt()).isEqualTo(Instant.parse("2026-10-20T05:00:00Z"));
    }

    @Test
    void rescheduleRevivesACancelledReminder() {
        Reminder reminder = reminder();
        reminder.markCancelled();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);

        reminder.reschedule(LocalDate.of(2026, 10, 15), IST, LocalTime.of(9, 0));

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void updatingJobDetailsLeavesTheScheduleAndStatusAlone() {
        Reminder reminder = reminder();
        Instant before = reminder.getRemindAt();
        reminder.markCancelled();

        reminder.updateJobDetails("Senior Engineer", "XYZ Ltd", "https://example.com/job/1");

        assertThat(reminder.getJobTitle()).isEqualTo("Senior Engineer");
        assertThat(reminder.getCompany()).isEqualTo("XYZ Ltd");
        assertThat(reminder.getJobUrl()).isEqualTo("https://example.com/job/1");
        assertThat(reminder.getRemindAt()).isEqualTo(before);
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
    }

    @Test
    void markSentRequiresATimestamp() {
        assertThatThrownBy(() -> reminder().markSent(null)).isInstanceOf(NullPointerException.class);
    }
}
