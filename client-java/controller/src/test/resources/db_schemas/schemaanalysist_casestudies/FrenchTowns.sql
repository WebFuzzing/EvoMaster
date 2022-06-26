DROP TABLE IF EXISTS Regions CASCADE;

DROP TABLE IF EXISTS Departments CASCADE;

DROP TABLE IF EXISTS Towns CASCADE;

CREATE TABLE Regions (
   id int UNIQUE NOT NULL,
   code VARCHAR(4) UNIQUE NOT NULL, 
   capital VARCHAR(10) NOT NULL, 
   name varchar(100) UNIQUE NOT NULL
);

CREATE TABLE Departments (
   id int UNIQUE NOT NULL,
   code VARCHAR(4) UNIQUE NOT NULL, 
   capital VARCHAR(10) UNIQUE NOT NULL, 
   region VARCHAR(4) NOT NULL REFERENCES Regions (code),
   name varchar(100) UNIQUE NOT NULL
);

CREATE TABLE Towns (
   id int UNIQUE NOT NULL,
   code VARCHAR(10) NOT NULL, 
   article varchar(100),
   name varchar(100) NOT NULL, 
   department VARCHAR(4) NOT NULL REFERENCES Departments (code),
   UNIQUE (code, department)
);


