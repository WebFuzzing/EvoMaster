-- Add the teletan_type column to the tan table
ALTER TABLE tan
    ADD COLUMN teletan_type VARCHAR(10) NULL;

-- Add the teletan_type column to the app_session table
ALTER TABLE app_session
    ADD COLUMN teletan_type VARCHAR(10) NULL;
