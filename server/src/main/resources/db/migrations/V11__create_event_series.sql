-- ES-001: recurring pattern template
CREATE TABLE event_series (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pattern_type        TEXT NOT NULL CHECK (pattern_type IN ('daily', 'weekly', 'custom')),
    weekdays            TEXT,                -- comma-separated ints, e.g. "0,2,4" (0=Mon…6=Sun)
    interval_days       INT,
    series_start_date   DATE NOT NULL,
    series_end_date     DATE,
    template_start_time TIME NOT NULL,
    template_end_time   TIME NOT NULL,
    created_by          UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
