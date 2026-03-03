CREATE TABLE notification_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    new_events BOOLEAN NOT NULL DEFAULT true,
    event_changes BOOLEAN NOT NULL DEFAULT true,
    event_cancellations BOOLEAN NOT NULL DEFAULT true,
    reminders BOOLEAN NOT NULL DEFAULT true,
    reminder_lead_time TEXT NOT NULL DEFAULT '1d',
    attendance_per_response BOOLEAN NOT NULL DEFAULT true,
    attendance_summary BOOLEAN NOT NULL DEFAULT false,
    attendance_summary_lead_time TEXT NOT NULL DEFAULT '2h',
    abwesenheit_changes BOOLEAN NOT NULL DEFAULT true
);
