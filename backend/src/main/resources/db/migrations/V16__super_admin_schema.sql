-- SA-001: super_admin flag on users
ALTER TABLE users ADD COLUMN super_admin BOOLEAN NOT NULL DEFAULT false;

-- Evolve club_managers to support SA invite/pending flow
ALTER TABLE club_managers ADD COLUMN id UUID DEFAULT gen_random_uuid();
ALTER TABLE club_managers ADD COLUMN invited_email TEXT;
ALTER TABLE club_managers ADD COLUMN status TEXT NOT NULL DEFAULT 'active';
ALTER TABLE club_managers ADD COLUMN accepted_at TIMESTAMPTZ;

-- Backfill invited_email from existing user rows
UPDATE club_managers cm SET invited_email = u.email FROM users u WHERE cm.user_id = u.id;
ALTER TABLE club_managers ALTER COLUMN invited_email SET NOT NULL;
ALTER TABLE club_managers ADD CONSTRAINT club_managers_status_check
    CHECK (status IN ('pending', 'active'));

-- New primary key (id) replacing composite (club_id, user_id)
ALTER TABLE club_managers DROP CONSTRAINT club_managers_pkey;
ALTER TABLE club_managers ADD PRIMARY KEY (id);

-- user_id may be null for pending invites
ALTER TABLE club_managers ALTER COLUMN user_id DROP NOT NULL;

-- Unique: one entry per email per club
ALTER TABLE club_managers
    ADD CONSTRAINT uq_club_managers_club_email UNIQUE (club_id, invited_email);

-- Clubs: metadata JSONB + soft-delete guard
ALTER TABLE clubs ADD COLUMN metadata JSONB;
ALTER TABLE clubs ADD COLUMN deleted_at TIMESTAMPTZ;

-- SA-002: audit_log (append-only; app role should have no UPDATE/DELETE granted at infra level)
CREATE TABLE audit_log (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id                 UUID        NOT NULL REFERENCES users(id),
    action                   TEXT        NOT NULL,
    target_type              TEXT,
    target_id                UUID,
    payload                  JSONB,
    impersonated_as          UUID        REFERENCES users(id),
    impersonation_session_id UUID,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- SA-003: impersonation_sessions
CREATE TABLE impersonation_sessions (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    superadmin_id  UUID        NOT NULL REFERENCES users(id),
    manager_id     UUID        NOT NULL REFERENCES users(id),
    club_id        UUID        NOT NULL REFERENCES clubs(id),
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ NOT NULL,
    ended_at       TIMESTAMPTZ
);

-- SA-004: export_jobs
CREATE TABLE export_jobs (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type         TEXT        NOT NULL,
    actor_id     UUID        NOT NULL REFERENCES users(id),
    status       TEXT        NOT NULL DEFAULT 'pending',
    filters      JSONB,
    result_path  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT export_jobs_status_check CHECK (status IN ('pending', 'running', 'done', 'failed'))
);

-- SA-005: indexes
CREATE INDEX audit_log_actor_id_idx ON audit_log(actor_id);
CREATE INDEX audit_log_created_at_idx ON audit_log(created_at);
CREATE INDEX impersonation_sessions_sa_ended_idx
    ON impersonation_sessions(superadmin_id, ended_at);
