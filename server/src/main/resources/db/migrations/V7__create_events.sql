CREATE TABLE event_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pattern_type TEXT NOT NULL CHECK (pattern_type IN ('daily', 'weekly', 'custom')),
    weekdays SMALLINT[] NULL,
    interval_days INT NULL,
    series_start_date DATE NOT NULL,
    series_end_date DATE NULL,
    template_start_time TIME NOT NULL,
    template_end_time TIME NOT NULL,
    template_meetup_time TIME NULL,
    template_title TEXT NOT NULL,
    template_type TEXT NOT NULL CHECK (template_type IN ('training', 'match', 'other')),
    template_location TEXT NULL,
    template_description TEXT NULL,
    template_min_attendees INT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('training', 'match', 'other')),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    meetup_at TIMESTAMPTZ NULL,
    location TEXT NULL,
    description TEXT NULL,
    min_attendees INT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'cancelled')),
    cancelled_at TIMESTAMPTZ NULL,
    series_id UUID NULL REFERENCES event_series(id),
    series_sequence INT NULL,
    series_override BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_teams (
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, team_id)
);

CREATE TABLE event_subgroups (
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    subgroup_id UUID NOT NULL REFERENCES sub_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, subgroup_id)
);

CREATE INDEX idx_events_series ON events(series_id, series_sequence);
CREATE INDEX idx_events_start_status ON events(start_at, status);
CREATE INDEX idx_event_teams_team ON event_teams(team_id, event_id);
CREATE INDEX idx_event_subgroups_event ON event_subgroups(event_id);
