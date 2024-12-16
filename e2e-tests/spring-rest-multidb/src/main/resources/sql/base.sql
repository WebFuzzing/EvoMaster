CREATE SCHEMA IF NOT EXISTS foo;

CREATE TABLE IF NOT EXISTS foo.BaseTable(
    id VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    constraint pk_x primary key (id)
);