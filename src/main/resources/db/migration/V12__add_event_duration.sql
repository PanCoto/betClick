ALTER TABLE events ADD COLUMN expected_duration_minutes INT NOT NULL DEFAULT 90;
COMMENT ON COLUMN events.expected_duration_minutes IS 'Przewidywany czas trwania w minutach (od start_time)';
