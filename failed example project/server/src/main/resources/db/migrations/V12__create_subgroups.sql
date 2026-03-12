-- ES-005: team sub-groups
CREATE TABLE subgroups (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id    UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ES-006: sub-group membership
CREATE TABLE subgroup_members (
    subgroup_id UUID NOT NULL REFERENCES subgroups(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (subgroup_id, user_id)
);
