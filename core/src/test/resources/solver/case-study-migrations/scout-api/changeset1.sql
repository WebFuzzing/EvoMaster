-- ChangeSet 1: Create system_message table
CREATE TABLE system_message (
                     id INT AUTO_INCREMENT,
                     `key` VARCHAR(100) NOT NULL,
                     `value` VARCHAR(100) NOT NULL,
                     valid_from DATETIME,
                     valid_to DATETIME,
                     PRIMARY KEY (id),
                     UNIQUE (`key`)
);

-- Create tag table
CREATE TABLE tag (
                     id INT AUTO_INCREMENT PRIMARY KEY,
                     grp VARCHAR(50) NOT NULL,
                     name NVARCHAR(50) NOT NULL
);

-- Create unique index on tag (grp, name)
CREATE UNIQUE INDEX tag_ix_grp_name ON tag (grp, name);

-- Create media_file table
CREATE TABLE media_file (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100),
                            mime_type VARCHAR(100),
                            uri VARCHAR(500) NOT NULL UNIQUE
);

-- Create users table
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) NOT NULL,
                       email_address VARCHAR(500),
                       authorization_level INTEGER NOT NULL,
                       date_created DATETIME NOT NULL
);

-- Create user_identity table
CREATE TABLE user_identity (
                               id INT AUTO_INCREMENT PRIMARY KEY,
                               type NVARCHAR(10) NOT NULL,
                               `value` NVARCHAR(500) NOT NULL,
                               date_created DATETIME NOT NULL,
                               user_id INT NOT NULL,
                               FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create unique index on user_identity (type, value)
CREATE UNIQUE INDEX user_identity_ix_type_value
ON user_identity (type, `value`);

-- Create activity table
CREATE TABLE activity (
                          id INT AUTO_INCREMENT PRIMARY KEY
);

-- Create ACTIVITY_RATING table
CREATE TABLE ACTIVITY_RATING (
                                 activity_id INT NOT NULL,
                                 user_id INT NOT NULL,
                                 rating INTEGER,
                                 favourite BOOLEAN,
                                 PRIMARY KEY (activity_id, user_id),
                                 FOREIGN KEY (activity_id) REFERENCES activity(id),
                                 FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create non-unique index on ACTIVITY_RATING (rating)
CREATE INDEX ACTIVITY_RATING_idx_rating ON ACTIVITY_RATING (rating);

-- Create ACTIVITY_RELATION table
CREATE TABLE ACTIVITY_RELATION (
                                   activity_1_id INT NOT NULL,
                                   activity_2_id INT NOT NULL,
                                   owner_id INT NOT NULL,
                                   PRIMARY KEY (activity_1_id, activity_2_id),
                                   FOREIGN KEY (activity_1_id) REFERENCES activity(id),
                                   FOREIGN KEY (activity_2_id) REFERENCES activity(id),
                                   FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Create activity_derived view
CREATE VIEW activity_derived AS
SELECT
    A.ID AS ACTIVITY_ID,
    SUM(CASE WHEN AUR.FAVOURITE THEN 1 ELSE 0 END) AS FAVOURITES_COUNT,
    SUM(AUR.RATING) AS RATINGS_SUM,
    COUNT(AUR.RATING) AS RATINGS_COUNT,
    AVG(AUR.RATING) AS RATINGS_AVG
FROM
    ACTIVITY A LEFT JOIN ACTIVITY_RATING AUR ON A.ID = AUR.ACTIVITY_ID
GROUP BY
    A.ID;

-- Create activity_properties table
CREATE TABLE activity_properties (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     date_created DATETIME,
                                     date_published DATETIME,
                                     date_updated DATETIME,
                                     name VARCHAR(100) NOT NULL,
                                     description_introduction VARCHAR(20000),
                                     description_main VARCHAR(20000),
                                     description_material VARCHAR(20000),
                                     description_notes VARCHAR(20000),
                                     description_prepare VARCHAR(20000),
                                     description_safety VARCHAR(20000),
                                     featured BOOLEAN,
                                     age_max INTEGER,
                                     age_min INTEGER,
                                     participants_max INTEGER,
                                     participants_min INTEGER,
                                     time_max INTEGER,
                                     time_min INTEGER,
                                     source VARCHAR(2000),
                                     activity_id INT NOT NULL,
                                     publishing_activity_id INT UNIQUE,
                                     author_id INT NOT NULL,
                                     FOREIGN KEY (activity_id) REFERENCES activity(id),
                                     FOREIGN KEY (publishing_activity_id) REFERENCES activity(id),
                                     FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Create activity_properties_tag table
CREATE TABLE activity_properties_tag (
                                         activity_properties_id INT NOT NULL,
                                         tag_id INT NOT NULL,
                                         PRIMARY KEY (activity_properties_id, tag_id),
                                         FOREIGN KEY (activity_properties_id) REFERENCES activity_properties(id),
                                         FOREIGN KEY (tag_id) REFERENCES tag(id)
);

-- Create activity_properties_media_file table
CREATE TABLE activity_properties_media_file (
                                                activity_properties_id INT NOT NULL,
                                                media_file_id INT NOT NULL,
                                                featured BOOLEAN,
                                                PRIMARY KEY (activity_properties_id, media_file_id),
                                                FOREIGN KEY (activity_properties_id) REFERENCES activity_properties(id),
                                                FOREIGN KEY (media_file_id) REFERENCES media_file(id)
);