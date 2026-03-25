CREATE TABLE users (
   id BIGINT NOT NULL AUTO_INCREMENT,
   username VARCHAR(255) NOT NULL UNIQUE,
   password VARCHAR(255) NOT NULL,
   PRIMARY KEY (id)
);

CREATE TABLE hibernate_sequence (
   next_val BIGINT
);

INSERT INTO hibernate_sequence VALUES (1);
