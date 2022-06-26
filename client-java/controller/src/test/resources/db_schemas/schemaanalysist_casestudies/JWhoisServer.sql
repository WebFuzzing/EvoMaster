DROP TABLE IF EXISTS nameserver;
DROP TABLE IF EXISTS inetnum;
DROP TABLE IF EXISTS domain;
DROP TABLE IF EXISTS person;
DROP TABLE IF EXISTS mntnr;
DROP TABLE IF EXISTS type;
DROP TABLE IF EXISTS country;

CREATE TABLE domain (
       domain_key INTEGER NOT NULL
     , domain VARCHAR(255) NOT NULL
     , registered_date TIMESTAMP NOT NULL
     , registerexpire_date TIMESTAMP NOT NULL
     , changed TIMESTAMP(19) DEFAULT CURRENT_TIMESTAMP NOT NULL
     , remarks VARCHAR(255)
     , holder INTEGER NOT NULL
     , admin_c INTEGER NOT NULL
     , tech_c INTEGER NOT NULL
     , zone_c INTEGER NOT NULL
     , mntnr_fkey INTEGER NOT NULL
     , publicviewabledata SMALLINT DEFAULT 1 NOT NULL
     , disabled SMALLINT DEFAULT 0 NOT NULL
     , PRIMARY KEY (domain_key)
);

CREATE TABLE mntnr (
       mntnr_key INTEGER NOT NULL
     , login VARCHAR(255) NOT NULL
     , password VARCHAR(255) NOT NULL
     , name VARCHAR(255) NOT NULL
     , address VARCHAR(255) NOT NULL
     , pcode VARCHAR(20) NOT NULL
     , city VARCHAR(255) NOT NULL
     , country_fkey INTEGER DEFAULT 0 NOT NULL
     , phone VARCHAR(100) NOT NULL
     , fax VARCHAR(100)
     , email VARCHAR(255) NOT NULL
     , remarks VARCHAR(255)
     , changed TIMESTAMP(19) DEFAULT CURRENT_TIMESTAMP NOT NULL
     , disabled SMALLINT DEFAULT 0 NOT NULL
     , PRIMARY KEY (mntnr_key)
);

CREATE TABLE person (
       person_key INTEGER NOT NULL
     , type_fkey INTEGER DEFAULT 0 NOT NULL
     , name VARCHAR(255) NOT NULL
     , address VARCHAR(255) NOT NULL
     , pcode VARCHAR(20) NOT NULL
     , city VARCHAR(255) NOT NULL
     , country_fkey INTEGER DEFAULT 0 NOT NULL
     , phone VARCHAR(100) NOT NULL
     , fax VARCHAR(100)
     , email VARCHAR(255) NOT NULL
     , remarks VARCHAR(255)
     , changed TIMESTAMP(19) DEFAULT CURRENT_TIMESTAMP NOT NULL
     , mntnr_fkey INTEGER DEFAULT 0 NOT NULL
     , disabled SMALLINT DEFAULT 0 NOT NULL
     , PRIMARY KEY (person_key)
);

CREATE TABLE type (
       type_key INTEGER NOT NULL
     , type VARCHAR(100) NOT NULL
     , PRIMARY KEY (type_key)
);

CREATE TABLE nameserver (
       nameserver_key INTEGER NOT NULL
     , nameserver VARCHAR(255) NOT NULL
     , domain_fkey INTEGER NOT NULL
     , PRIMARY KEY (nameserver_key)
);

CREATE TABLE country (
       country_key INTEGER NOT NULL
     , short VARCHAR(2) NOT NULL
     , country VARCHAR(255) NOT NULL
     , PRIMARY KEY (country_key)
);
