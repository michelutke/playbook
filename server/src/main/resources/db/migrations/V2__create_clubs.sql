CREATE TABLE clubs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    logo_url    TEXT,
    sport_type  TEXT NOT NULL,
    location    TEXT,
    status      TEXT NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'inactive')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Now add FK from club_managers to clubs
ALTER TABLE club_managers
    ADD CONSTRAINT fk_club_managers_club
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE;
