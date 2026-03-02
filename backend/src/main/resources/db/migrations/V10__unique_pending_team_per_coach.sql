-- Prevent concurrent coach link joins from creating duplicate pending teams.
-- A coach may have at most one pending team per club at a time.
-- Historical archived/rejected teams are unaffected (partial index).
CREATE UNIQUE INDEX uq_pending_team_per_coach
    ON teams (club_id, requested_by)
    WHERE status = 'pending';
