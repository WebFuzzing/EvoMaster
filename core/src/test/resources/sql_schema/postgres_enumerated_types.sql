CREATE TYPE mood AS ENUM ('sad','ok','happy');

CREATE TABLE EnumeratedTypes (
  enumColumn mood NOT NULL
);

