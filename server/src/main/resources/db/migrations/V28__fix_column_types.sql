-- Fix weekdays column type: smallint[] → text (stored as comma-separated, e.g. "0,1,2")
ALTER TABLE abwesenheit_rules
    ALTER COLUMN weekdays TYPE TEXT USING array_to_string(weekdays, ',');

-- Fix audit_log payload column type: jsonb → text (Exposed uses text column)
ALTER TABLE audit_log
    ALTER COLUMN payload TYPE TEXT;
