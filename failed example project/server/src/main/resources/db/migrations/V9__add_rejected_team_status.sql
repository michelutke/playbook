-- Add 'rejected' as a valid team status (distinct from 'archived')
ALTER TABLE teams
    DROP CONSTRAINT teams_status_check;

ALTER TABLE teams
    ADD CONSTRAINT teams_status_check
    CHECK (status IN ('active', 'archived', 'pending', 'rejected'));
