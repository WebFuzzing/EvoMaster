-- Create the tan table
CREATE TABLE tan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    tan_hash VARCHAR(64),
    sot VARCHAR(255),
    type VARCHAR(255),
    redeemed BOOLEAN,
    PRIMARY KEY (id)
);

-- Create index for tan_hash
CREATE INDEX idx_tan_tan_hash ON tan (tan_hash);
