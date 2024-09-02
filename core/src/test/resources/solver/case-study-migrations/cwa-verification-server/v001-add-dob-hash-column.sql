-- Add the hashed_guid_dob column to the app_session table
ALTER TABLE app_session
    ADD COLUMN hashed_guid_dob VARCHAR(64) NULL;
