package com.jobcrm.reminders.reminder;

/**
 * Lifecycle of a reminder. The names are stored as text and must match the CHECK constraint in
 * the database migration (a test guards this).
 */
public enum ReminderStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    CANCELLED
}
