CREATE TABLE invite_links (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  token TEXT UNIQUE NOT NULL,
  team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  invited_by_user_id UUID NOT NULL REFERENCES users(id),
  invited_email TEXT,
  role TEXT NOT NULL DEFAULT 'player',
  expires_at TIMESTAMPTZ NOT NULL,
  redeemed_at TIMESTAMPTZ,
  redeemed_by_user_id UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invites_token ON invite_links(token);
CREATE INDEX idx_invites_team ON invite_links(team_id);
