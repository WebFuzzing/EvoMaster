CREATE TABLE Parent (
    id1 integer NOT NULL,
    id2 integer NOT NULL,
    name varchar(255),
    PRIMARY KEY (id1, id2)
);

CREATE TABLE Child (
    child_id integer PRIMARY KEY,
    parent_id1 integer NOT NULL,
    parent_id2 integer NOT NULL,
    description varchar(255),
    FOREIGN KEY (parent_id1, parent_id2) REFERENCES Parent(id1, id2)
);
