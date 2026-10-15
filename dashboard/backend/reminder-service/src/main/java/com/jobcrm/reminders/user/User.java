package com.jobcrm.reminders.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A person who receives reminders, identified by a verified email address.
 * Verification itself (email OTP) is added in Part 6.
 */
@Entity
@Table(name = "users")
public class User {

    public static final int EMAIL_MAX_LENGTH = 254;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** For JPA only. */
    protected User() {}

    public User(String email) {
        this.email = normalizeEmail(email);
    }

    /** Emails are stored trimmed and lower-case; the database enforces the same rule. */
    public static String normalizeEmail(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (normalized.length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException("email must be at most " + EMAIL_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    /** Records the first time the address was verified; later calls keep the original time. */
    public void markVerified(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        if (this.verifiedAt == null) {
            this.verifiedAt = at;
        }
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

    public String getEmail() {
        return email;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
