create table X (
  id BIGINT,
  textColumn TEXT NOT NULL,
  dateColumn DATE NOT NULL,
  uuidColumn UUID NOT NULL,
  jsonbColumn JSONB NOT NULL,
  xmlColumn XML NOT NULL,
  w_id TEXT NOT NULL,
  f_id TEXT NOT NULL,
  p_at               TIMESTAMP,
  status             TEXT NOT NULL,
  constraint pk_x primary key (id)
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE x ADD CONSTRAINT check_f_id
  CHECK (   f_id LIKE 'hi'
    OR f_id LIKE '%foo%'
    OR f_id LIKE '%foo%x%'
    OR f_id LIKE '%bar%'
    OR f_id LIKE '%bar%y%'
    OR f_id LIKE '%hello%');

ALTER TABLE x ADD CONSTRAINT check_w_id
  CHECK (w_id SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?');

ALTER TABLE x ADD CONSTRAINT check_status
  CHECK (status in ('A', 'B'));

ALTER TABLE x ADD CONSTRAINT check_status_at
  CHECK ((status = 'B') = (p_at IS NOT NULL));
