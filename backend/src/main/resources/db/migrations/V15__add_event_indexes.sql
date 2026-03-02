-- ES-007: indexes for event scheduling queries
CREATE INDEX events_series_id_sequence ON events(series_id, series_sequence);
CREATE INDEX events_start_at_status ON events(start_at, status);
CREATE INDEX event_teams_team_id_event_id ON event_teams(team_id, event_id);
CREATE INDEX event_subgroups_event_id ON event_subgroups(event_id);
CREATE INDEX subgroup_members_user_id ON subgroup_members(user_id);
