CREATE TABLE x
(
  w_id               TEXT NOT NULL
);


ALTER TABLE x ADD CONSTRAINT check_w_id
  CHECK (w_id SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?');




