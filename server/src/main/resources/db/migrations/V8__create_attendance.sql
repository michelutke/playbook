-- V8: Attendance tracking tables + events columns + team_roles FK fix (TM-19)

-- 1. Add attendance columns to events
ALTER TABLE events
    ADD COLUMN response_deadline TIMESTAMPTZ NULL,
    ADD COLUMN check_in_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Fix team_roles FK (TM-19): user_id CASCADE -> SET NULL to preserve historical attendance data
--    Must allow NULL first, then re-add FK with SET NULL
ALTER TABLE team_roles
    DROP CONSTRAINT team_roles_user_id_fkey;

ALTER TABLE team_roles
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE team_roles
    ADD CONSTRAINT team_roles_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 3. attendance_responses: one row per (event, user), stores RSVP status
CREATE TABLE attendance_responses (
    event_id            UUID    NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id             UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              TEXT    NOT NULL DEFAULT 'no-response'
                                CHECK (status IN ('confirmed', 'declined', 'unsure', 'declined-auto', 'no-response')),
    reason              TEXT    NULL,
    abwesenheit_rule_id UUID    NULL,
    manual_override     BOOLEAN NOT NULL DEFAULT FALSE,
    responded_at        TIMESTAMPTZ NULL,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id)
);

-- Add FK for abwesenheit_rule_id after abwesenheit_rules table is created (below)

-- 4. attendance_records: post-event actual attendance (present/absent/excused)
CREATE TABLE attendance_records (
    event_id            UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              TEXT NOT NULL CHECK (status IN ('present', 'absent', 'excused')),
    note                TEXT NULL,
    set_by              UUID NOT NULL REFERENCES users(id),
    set_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    previous_status     TEXT NULL CHECK (previous_status IS NULL OR previous_status IN ('present', 'absent', 'excused')),
    previous_set_by     UUID NULL REFERENCES users(id),
    PRIMARY KEY (event_id, user_id)
);

-- 5. abwesenheit_rules: recurring/period absence rules per user
CREATE TABLE abwesenheit_rules (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preset_type  TEXT NOT NULL CHECK (preset_type IN ('holidays', 'injury', 'work', 'school', 'travel', 'other')),
    label        TEXT NOT NULL,
    body_part    TEXT NULL,
    rule_type    TEXT NOT NULL CHECK (rule_type IN ('recurring', 'period')),
    weekdays     SMALLINT[] NULL,
    start_date   DATE NULL,
    end_date     DATE NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. Add FK from attendance_responses to abwesenheit_rules (now that the table exists)
ALTER TABLE attendance_responses
    ADD CONSTRAINT attendance_responses_abwesenheit_rule_id_fkey
        FOREIGN KEY (abwesenheit_rule_id) REFERENCES abwesenheit_rules(id) ON DELETE SET NULL;

-- 7. Indexes
CREATE INDEX idx_attendance_responses_user ON attendance_responses(user_id, event_id);
CREATE INDEX idx_attendance_records_event  ON attendance_records(event_id);
CREATE INDEX idx_abwesenheit_rules_user    ON abwesenheit_rules(user_id);
CREATE INDEX idx_events_team_start         ON events(start_at) WHERE status = 'active';
