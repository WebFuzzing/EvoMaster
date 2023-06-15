CREATE TABLE email_table
(
  email_column               TEXT NOT NULL,
  CONSTRAINT email_format CHECK (email_column SIMILAR TO '[A-Za-z]{2,}@[A-Za-z]+.[A-Za-z]{2,}'),
  CONSTRAINT email_domain CHECK (email_column SIMILAR TO '%@(gmail|yahoo|hotmail)%')
);





