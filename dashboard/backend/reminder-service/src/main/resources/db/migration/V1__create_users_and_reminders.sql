-- V1: users and reminders (Part 2).
--
-- Flyway migrations are append-only: never edit this file once it has been applied anywhere.
-- Later changes (e.g. retry/lease columns for the scheduler in Part 4) go into V2, V3, ...
--
-- Requires PostgreSQL 13+ (gen_random_uuid() is built in from 13).
--
-- Not enforced here on purpose:
--   * remind_at is derived by the application from follow_up_date + send_time + timezone.
--     A database CHECK could disagree with Java's time-zone rules around DST gaps.
--   * timezone validity (IANA name) is checked by the application.

CREATE TABLE users (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    email       VARCHAR(254) NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    -- Emails are stored trimmed and lower-case so uniqueness is case-insensitive.
    CONSTRAINT ck_users_email_normalized
        CHECK (length(email) > 0 AND email = btrim(email) AND email = lower(email))
);

CREATE TABLE reminders (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL,
    -- The extension's stable job id (Job.id, a UUID). Unique per user: one reminder per job.
    client_job_id  UUID         NOT NULL,
    job_title      VARCHAR(200) NOT NULL,
    company        VARCHAR(200) NOT NULL,
    job_url        VARCHAR(2048),
    -- Calendar date picked by the user, no time zone (yyyy-MM-dd in the extension).
    follow_up_date DATE         NOT NULL,
    timezone       VARCHAR(64)  NOT NULL DEFAULT 'Asia/Kolkata',
    send_time      TIME         NOT NULL DEFAULT TIME '09:00',
    -- Exact send instant, computed by the server. Always UTC-based (timestamptz).
    remind_at      TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    sent_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_reminders PRIMARY KEY (id),
    -- Deleting a user deletes their reminders (account deletion, Part 8).
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Idempotent upsert key for PUT /api/reminders/{clientJobId}. Also indexes user_id lookups.
    CONSTRAINT uq_reminders_user_client_job UNIQUE (user_id, client_job_id),
    CONSTRAINT ck_reminders_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_reminders_sent_has_timestamp
        CHECK (status <> 'SENT' OR sent_at IS NOT NULL),
    CONSTRAINT ck_reminders_job_title_not_blank CHECK (length(btrim(job_title)) > 0),
    CONSTRAINT ck_reminders_company_not_blank CHECK (length(btrim(company)) > 0),
    CONSTRAINT ck_reminders_job_url_not_blank CHECK (job_url IS NULL OR length(btrim(job_url)) > 0),
    CONSTRAINT ck_reminders_timezone_not_blank CHECK (length(btrim(timezone)) > 0)
);

-- The scheduler (Part 4) repeatedly asks: which PENDING reminders are due? A partial index keeps
-- that query fast and small: rows leave the index once they are sent, failed or cancelled.
CREATE INDEX ix_reminders_due_pending ON reminders (remind_at) WHERE status = 'PENDING';
