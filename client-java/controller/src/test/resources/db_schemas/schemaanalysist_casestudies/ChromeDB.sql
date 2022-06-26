CREATE TABLE Databases (
    id INTEGER PRIMARY KEY,
    origin TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    estimated_size INTEGER NOT NULL
);

CREATE TABLE meta(
    key LONGVARCHAR NOT NULL UNIQUE PRIMARY KEY,
    value LONGVARCHAR
);

CREATE INDEX origin_index ON Databases (origin);
CREATE UNIQUE INDEX unique_index ON Databases (origin, name);

