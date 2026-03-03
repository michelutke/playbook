CREATE INDEX attendance_responses_user_event ON attendance_responses(user_id, event_id);
CREATE INDEX attendance_records_event_id ON attendance_records(event_id);
CREATE INDEX abwesenheit_rules_user_id ON abwesenheit_rules(user_id);
CREATE INDEX abwesenheit_backfill_jobs_rule_id ON abwesenheit_backfill_jobs(rule_id);
