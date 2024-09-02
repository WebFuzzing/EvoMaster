-- ChangeSet 3: Create MEDIA_FILE_KEYWORDS table
CREATE TABLE MEDIA_FILE_KEYWORDS (
                                     media_file_id INT NOT NULL,
                                     keyword VARCHAR(100) NOT NULL,
                                     PRIMARY KEY (media_file_id, keyword),
                                     FOREIGN KEY (media_file_id) REFERENCES media_file(id)
);

-- Add columns to MEDIA_FILE table
ALTER TABLE MEDIA_FILE
    ADD COLUMN capture_date DATETIME;

ALTER TABLE MEDIA_FILE
    ADD COLUMN copy_right VARCHAR(100);

ALTER TABLE MEDIA_FILE
    ADD COLUMN author VARCHAR(50);


