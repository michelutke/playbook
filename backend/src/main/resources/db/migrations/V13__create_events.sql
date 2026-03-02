-- ES-002: events
CREATE TABLE events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            TEXT NOT NULL,
    type             TEXT NOT NULL CHECK (type IN ('training', 'match', 'other')),
    start_at         TIMESTAMPTZ NOT NULL,
    end_at           TIMESTAMPTZ NOT NULL,
    meetup_at        TIMESTAMPTZ,
    location         TEXT,
    description      TEXT,
    min_attendees    INT,
    status           TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'cancelled')),
    cancelled_at     TIMESTAMPTZ,
    series_id        UUID REFERENCES event_series(id) ON DELETE SET NULL,
    series_sequence  INT,
    series_override  BOOLEAN NOT NULL DEFAULT false,
    created_by       UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
