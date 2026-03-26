CREATE TABLE notifications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type             TEXT NOT NULL,
    title            TEXT NOT NULL,
    body             TEXT NOT NULL,
    entity_id        UUID NULL,
    entity_type      TEXT NULL,
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key  TEXT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX notifications_dedup ON notifications(user_id, idempotency_key);
CREATE INDEX notifications_user_unread ON notifications(user_id, is_read, created_at DESC);

CREATE TABLE notification_settings (
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id              UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    events_new           BOOLEAN NOT NULL DEFAULT TRUE,
    events_edit          BOOLEAN NOT NULL DEFAULT TRUE,
    events_cancel        BOOLEAN NOT NULL DEFAULT TRUE,
    reminders_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_lead_minutes INTEGER NOT NULL DEFAULT 120,
    coach_response_mode  TEXT NOT NULL DEFAULT 'per_response'
                          CHECK (coach_response_mode IN ('per_response','summary')),
    absences_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (user_id, team_id)
);

CREATE TABLE event_reminder_overrides (
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id             UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    reminder_lead_minutes INTEGER NULL,
    PRIMARY KEY (user_id, event_id)
);

CREATE TABLE notification_reminders (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id   UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    fire_at    TIMESTAMPTZ NOT NULL,
    sent       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX notification_reminders_uniq ON notification_reminders(user_id, event_id);
CREATE INDEX notification_reminders_due ON notification_reminders(fire_at, sent) WHERE NOT sent;
