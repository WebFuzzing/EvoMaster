CREATE TABLE X
(
  ID                 UUID PRIMARY KEY,
  F_ID               TEXT NOT NULL,
  P_AT               TIMESTAMP,
  STATUS             TEXT CHECK (STATUS in ('A', 'B')),
  CREATED_AT         TIMESTAMP NOT NULL,
  CREATED_BY         TEXT NOT NULL,
  LAST_UPDATED_AT    TIMESTAMP,
  LAST_UPDATED_BY    TEXT,
  W_ID               TEXT NOT NULL,
  EXPR_DATE          DATE NOT NULL
);



