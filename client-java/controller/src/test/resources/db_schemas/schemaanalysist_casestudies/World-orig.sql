DROP TABLE IF EXISTS City CASCADE;

DROP TABLE IF EXISTS country CASCADE;

DROP TABLE IF EXISTS countrylanguage CASCADE;

CREATE TABLE city (
    id int PRIMARY KEY NOT NULL,
    name varchar(100) NOT NULL,
    countrycode character(3) NOT NULL,
    district varchar(100) NOT NULL,
    population integer NOT NULL
);

CREATE TABLE country (
    code varchar(3) PRIMARY KEY NOT NULL,
    name varchar(100) NOT NULL,
    continent varchar(100) NOT NULL,
    region varchar(100) NOT NULL,
    surfacearea int NOT NULL,
    indepyear int,
    population integer NOT NULL,
    lifeexpectancy int,
    gnp int,
    gnpold int,
    localname varchar(100) NOT NULL,
    governmentform varchar(100) NOT NULL,
    headofstate varchar(100),
    capital integer,
    code2 varchar(2) NOT NULL,
    CONSTRAINT country_continent_check CHECK ((((((((continent = 'Asia'::text) OR (continent = 'Europe'::text)) OR (continent = 'North America'::text)) OR (continent = 'Africa'::text)) OR (continent = 'Oceania'::text)) OR (continent = 'Antarctica'::text)) OR (continent = 'South America'::text)))
);

CREATE TABLE countrylanguage (
    countrycode character(3) NOT NULL,
    "language" varchar(100) NOT NULL,
    isofficial boolean NOT NULL,
    percentage real NOT NULL
);

ALTER TABLE ONLY countrylanguage
    ADD CONSTRAINT countrylanguage_pkey PRIMARY KEY (countrycode, "language");

ALTER TABLE ONLY country
    ADD CONSTRAINT country_capital_fkey FOREIGN KEY (capital) REFERENCES city(id);

ALTER TABLE ONLY countrylanguage
    ADD CONSTRAINT countrylanguage_countrycode_fkey FOREIGN KEY (countrycode) REFERENCES country(code);

