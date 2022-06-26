DROP TABLE Person;

CREATE TABLE person (
        id int not null,
        last_name varchar(45) not null,
        first_name varchar(45) not null,
        gender varchar(6) not null,
        date_of_birth date not null,
        PRIMARY KEY  (id),
	CHECK (gender IN ('Male', 'Female', 'Uknown'))
);