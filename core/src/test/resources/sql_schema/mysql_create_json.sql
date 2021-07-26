# https://dev.mysql.com/doc/refman/8.0/en/json.html
CREATE TABLE people
(
    id                INT PRIMARY KEY,
    jsonData          JSON NOT NULL
);