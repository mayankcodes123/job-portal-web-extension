package com.jobcrm.reminders.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * A scheduled email reminder for one job application of one user.
 *
 * <p>The extension owns the job; this record holds only what is needed to send the email.
 * {@code remindAt} is never set directly: it is always derived from
 * {@code followUpDate + sendTime + timezone}, so the three can never disagree.
 *
 * <p>The owner is stored as a plain {@code userId} (the database enforces the foreign key),
 * which keeps this class free of lazy-loading concerns.
 *
 * <p>Retry and lease fields for the scheduler are added in Part 4 (migration V2).
 */
@Entity
@Table(name = "reminders")
public class Reminder {

    public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";
    public static final LocalTime DEFAULT_SEND_TIME = LocalTime.of(9, 0);

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int COMPANY_MAX_LENGTH = 200;
    public static final int URL_MAX_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** The extension's stable job id ({@code Job.id}). Unique per user. */
    @Column(name = "client_job_id", nullable = false, updatable = false)
    private UUID clientJobId;

    @Column(name = "job_title", nullable = false, length = TITLE_MAX_LENGTH)
    private String jobTitle;

    @Column(name = "company", nullable = false, length = COMPANY_MAX_LENGTH)
    private String company;

    @Column(name = "job_url", length = URL_MAX_LENGTH)
    private String jobUrl;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "send_time", nullable = false)
    private LocalTime sendTime;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReminderStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** For JPA only. */
    protected Reminder() {}

    public Reminder(
            UUID userId,
            UUID clientJobId,
            String jobTitle,
            String company,
            String jobUrl,
            LocalDate followUpDate,
            ZoneId timezone,
            LocalTime sendTime) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.clientJobId = Objects.requireNonNull(clientJobId, "clientJobId must not be null");
        applyJobDetails(jobTitle, company, jobUrl);
        applySchedule(followUpDate, timezone, sendTime);
        this.status = ReminderStatus.PENDING;
    }

    /**
     * The instant a reminder fires: the given local date and time in the given zone.
     * Exposed so the API layer (Part 3) can reject past times with the same rule the model uses.
     */
    public static Instant computeRemindAt(LocalDate followUpDate, LocalTime sendTime, ZoneId timezone) {
        return ZonedDateTime.of(followUpDate, sendTime, timezone).toInstant();
    }

    /** Updates the descriptive fields only; does not touch the schedule or status. */
    public void updateJobDetails(String jobTitle, String company, String jobUrl) {
        applyJobDetails(jobTitle, company, jobUrl);
    }

    /**
     * Changes the schedule and re-arms the reminder: status goes back to PENDING and any
     * earlier sent time is cleared. Callers decide whether the schedule really changed.
     */
    public void reschedule(LocalDate followUpDate, ZoneId timezone, LocalTime sendTime) {
        applySchedule(followUpDate, timezone, sendTime);
        this.status = ReminderStatus.PENDING;
        this.sentAt = null;
    }

    public void markSent(Instant sentAt) {
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
        this.status = ReminderStatus.SENT;
    }

    public void markCancelled() {
        this.status = ReminderStatus.CANCELLED;
    }

    private void applyJobDetails(String jobTitle, String company, String jobUrl) {
        this.jobTitle = requireText(jobTitle, "jobTitle", TITLE_MAX_LENGTH);
        this.company = requireText(company, "company", COMPANY_MAX_LENGTH);
        this.jobUrl = optionalText(jobUrl, "jobUrl", URL_MAX_LENGTH);
    }

    private void applySchedule(LocalDate followUpDate, ZoneId timezone, LocalTime sendTime) {
        Objects.requireNonNull(followUpDate, "followUpDate must not be null");
        Objects.requireNonNull(timezone, "timezone must not be null");
        Objects.requireNonNull(sendTime, "sendTime must not be null");
        LocalTime minutePrecision = sendTime.truncatedTo(ChronoUnit.MINUTES);
        this.followUpDate = followUpDate;
        this.timezone = timezone.getId();
        this.sendTime = minutePrecision;
        this.remindAt = computeRemindAt(followUpDate, minutePrecision, timezone);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getClientJobId() {
        return clientJobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public LocalTime getSendTime() {
        return sendTime;
    }

    public Instant getRemindAt() {
        return remindAt;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
