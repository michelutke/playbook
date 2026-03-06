-- ES-003: events <-> teams
CREATE TABLE event_teams (
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    team_id  UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, team_id)
);

-- ES-004: events <-> subgroups (optional audience restriction)
CREATE TABLE event_subgroups (
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    subgroup_id UUID NOT NULL REFERENCES subgroups(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, subgroup_id)
);
