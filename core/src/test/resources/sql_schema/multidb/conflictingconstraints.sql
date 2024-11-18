CREATE SCHEMA IF NOT EXISTS first;
CREATE SCHEMA IF NOT EXISTS other;


create table first.Foo (
                           id bigint not null,
                           x bigint,
                           primary key (id)
);


create table other.Foo(
                          id bigint,
                          x bigint not null,
                          primary key (x)
);