CREATE TABLE attendance_records (
    event_id         UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status           TEXT NOT NULL CHECK (status IN ('present','absent','excused')),
    note             TEXT,
    set_by           UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    set_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    previous_status  TEXT CHECK (previous_status IN ('present','absent','excused')),
    previous_set_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (event_id, user_id)
);
