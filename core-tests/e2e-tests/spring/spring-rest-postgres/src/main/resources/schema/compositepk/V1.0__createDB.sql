CREATE TABLE CompositePK (
    id1 integer NOT NULL,
    id2 integer NOT NULL,
    name varchar(255),
    PRIMARY KEY (id1, id2)
);
