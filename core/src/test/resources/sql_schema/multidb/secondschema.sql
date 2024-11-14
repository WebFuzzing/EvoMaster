create table Foo (
   id bigint not null,
   primary key (id)
);

CREATE SCHEMA IF NOT EXISTS other;

create table other.Bar(
    id bigint not null,
    primary key (id)
);