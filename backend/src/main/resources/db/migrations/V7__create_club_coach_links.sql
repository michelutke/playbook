CREATE TABLE club_coach_links (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    club_id    UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    token      TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ
);
