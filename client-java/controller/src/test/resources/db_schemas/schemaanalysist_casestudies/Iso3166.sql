DROP TABLE IF EXISTS country CASCADE;

CREATE TABLE country (
    name varchar(100) NOT NULL,
    two_letter varchar(100) PRIMARY KEY,
    country_id integer NOT NULL
);
