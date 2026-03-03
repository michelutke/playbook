CREATE TABLE attendance_responses (
    event_id            UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              TEXT NOT NULL DEFAULT 'no-response' CHECK (status IN ('confirmed','declined','unsure','declined-auto','no-response')),
    reason              TEXT,
    abwesenheit_rule_id UUID REFERENCES abwesenheit_rules(id) ON DELETE SET NULL,
    manual_override     BOOLEAN NOT NULL DEFAULT false,
    responded_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id)
);
