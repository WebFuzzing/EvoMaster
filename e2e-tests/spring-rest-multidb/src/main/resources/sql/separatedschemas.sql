CREATE SCHEMA IF NOT EXISTS foo;
CREATE SCHEMA IF NOT EXISTS bar;

CREATE TABLE IF NOT EXISTS foo.EntityX(
     id VARCHAR(50) NOT NULL,
     constraint pk_x primary key (id)
);

CREATE TABLE IF NOT EXISTS bar.EntityY(
    id VARCHAR(50) NOT NULL,
    constraint pk_y primary key (id)
);