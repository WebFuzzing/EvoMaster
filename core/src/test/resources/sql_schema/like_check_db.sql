CREATE TABLE x
(
  f_id               TEXT NOT NULL
);


ALTER TABLE x ADD CONSTRAINT check_f_id
  CHECK (   f_id LIKE 'hi'
    OR f_id LIKE '%foo%'
    OR f_id LIKE '%foo%x%'
    OR f_id LIKE '%bar%'
    OR f_id LIKE '%bar%y%'
    OR f_id LIKE '%hello%');
