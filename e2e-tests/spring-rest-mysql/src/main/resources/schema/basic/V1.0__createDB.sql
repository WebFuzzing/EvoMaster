create table X (
  id INTEGER,
  constraint pk_x primary key (id),
  a Tinyint ZEROFILL not null,
  b INT UNSIGNED not null
);