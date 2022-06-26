-- http://labrosa.ee.columbia.edu/millionsong/pages/getting-dataset#subset

CREATE TABLE tableA (columnA int PRIMARY KEY);
CREATE UNIQUE INDEX ON tableA (columnA);
CREATE INDEX ON tableA (columnA);

CREATE TABLE tableB (columnB int PRIMARY KEY, columnC int);
CREATE UNIQUE INDEX indexName ON tableB (columnB, columnC);
CREATE INDEX ON tableB (columnB);