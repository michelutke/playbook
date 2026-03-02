CREATE TABLE invites (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_type   TEXT NOT NULL CHECK (invite_type IN ('team_player', 'team_coach')),
    team_id       UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    role          TEXT NOT NULL CHECK (role IN ('coach', 'player')),
    invited_email TEXT NOT NULL,
    invite_token  TEXT UNIQUE NOT NULL,
    status        TEXT NOT NULL DEFAULT 'pending'
                      CHECK (status IN ('pending', 'accepted', 'expired', 'revoked')),
    invited_by    UUID REFERENCES users(id),
    expires_at    TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days'),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at   TIMESTAMPTZ
);
