CREATE TABLE player_profiles (
    team_id       UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jersey_number INT,
    position      TEXT,
    PRIMARY KEY (team_id, user_id)
);
