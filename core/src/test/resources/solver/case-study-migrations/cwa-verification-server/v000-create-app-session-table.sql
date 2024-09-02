-- Create the app_session table
CREATE TABLE app_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    hashed_guid VARCHAR(64),
    registration_token_hash VARCHAR(64),
    tele_tan_hash VARCHAR(64),
    tan_counter INT,
    sot VARCHAR(255),
    PRIMARY KEY (id)
);

-- Create index for hashed_guid
CREATE INDEX idx_app_session_guid_hash ON app_session (hashed_guid);

-- Create index for registration_token_hash
CREATE INDEX idx_app_session_registration_token_hash ON app_session (registration_token_hash);

-- Create index for tele_tan_hash
CREATE INDEX idx_app_session_tele_tan_hash ON app_session (tele_tan_hash);
