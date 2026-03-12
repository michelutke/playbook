-- invite_token already unique from table definition, add explicit named index
CREATE INDEX idx_invites_email_team     ON invites(invited_email, team_id);
CREATE INDEX idx_memberships_team_user  ON team_memberships(team_id, user_id);
CREATE INDEX idx_clubs_status           ON clubs(status);
CREATE INDEX idx_teams_club_id          ON teams(club_id);
CREATE INDEX idx_teams_status           ON teams(status);
CREATE INDEX idx_coach_links_active     ON club_coach_links(club_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_club_managers_user     ON club_managers(user_id);
