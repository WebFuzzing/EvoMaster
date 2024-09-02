-- ChangeSet 4: Add media_file_id column to tag table
ALTER TABLE tag
    ADD COLUMN media_file_id INT NULL;
ALTER TABLE tag
    ADD FOREIGN KEY (media_file_id) REFERENCES media_file(id);
