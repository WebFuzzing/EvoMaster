create table IF NOT EXISTS existing_table  (
  id integer primary key,
  x1 integer,
  notBlank varchar,
  email varchar not null,
  negative integer not null,
  negative_or_zero integer not null,
  positive integer not null,
  positive_or_zero integer not null,
  decimal_min integer not null,
  decimal_max integer not null,
  size varchar not null,
  reg_exp varchar not null,
  big_decimal decimal not null
);