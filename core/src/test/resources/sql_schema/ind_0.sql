CREATE TABLE x
(
  id                 UUID PRIMARY KEY,
  f_id               TEXT NOT NULL,
  p_at               TIMESTAMP,
  status             TEXT NOT NULL,
  xmlData            XML  NOT NULL,
  created_at         TIMESTAMP NOT NULL,
  created_by         TEXT NOT NULL,
  last_updated_at    TIMESTAMP,
  last_updated_by    TEXT,
  w_id               TEXT NOT NULL,
  expr_date          DATE NOT NULL
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE x ADD CONSTRAINT check_status
  CHECK (status in ('A', 'B'));

ALTER TABLE x ADD CONSTRAINT check_status_at
  CHECK ((status = 'B') = (p_at IS NOT NULL));

ALTER TABLE x ADD CONSTRAINT check_f_id
  CHECK (   f_id LIKE 'hi'
    OR f_id LIKE '%foo%'
    OR f_id LIKE '%foo%x%'
    OR f_id LIKE '%bar%'
    OR f_id LIKE '%bar%y%'
    OR f_id LIKE '%hello%');

ALTER TABLE x ADD CONSTRAINT check_w_id
  CHECK (w_id SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?');

ALTER TABLE x ADD CONSTRAINT check_unique
  UNIQUE (w_id, expr_date, f_id, status, p_at);


CREATE UNIQUE INDEX a_index
  ON x (w_id, expr_date, f_id) WHERE status = 'A';



CREATE OR REPLACE VIEW view_0 AS
  WITH z AS (
    SELECT   w_id, expr_date, f_id, max(p_at) AS p__at
    FROM     x
    WHERE    status = 'B'
    GROUP BY w_id, expr_date, f_id
    )
    SELECT c.*
    FROM x c
           INNER JOIN z lv
                      ON    lv.w_id     = c.w_id
                        AND lv.expr_date   = c.expr_date
                        AND lv.f_id = c.f_id
                        AND lv.p__at IS NOT DISTINCT FROM c.p_at
;

CREATE OR REPLACE VIEW view_1 AS
  SELECT *
  FROM   view_0
  UNION ALL
  SELECT *
  FROM   x
  WHERE  status = 'A'
;


CREATE OR REPLACE VIEW view_2 AS
  WITH z AS (
    SELECT   w_id, f_id, max(p_at) AS p__at
    FROM     x
    WHERE    status = 'B'
    GROUP BY w_id, f_id
    )
    SELECT c.*
    FROM x c
           INNER JOIN z lv
                      ON    lv.w_id     = c.w_id
                        AND lv.f_id = c.f_id
                        AND lv.p__at IS NOT DISTINCT FROM c.p_at
;


CREATE TABLE y
(
  e_id     TEXT NOT NULL,
  f_id     TEXT NOT NULL,
  status   TEXT NOT NULL,
  q_at     TIMESTAMP,
  s_at     TIMESTAMP,
  c_at     TIMESTAMP,
  jsonData jsonb,
  PRIMARY KEY (e_id, f_id),
  CONSTRAINT check_status CHECK (status IN ('A', 'B', 'C', 'D', 'E'))
);




