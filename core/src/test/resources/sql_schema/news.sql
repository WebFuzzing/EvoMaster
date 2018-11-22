create sequence hibernate_sequence
  start with 1 increment by 1;

create table news_entity (
  id bigint not null, author_id varchar(32) not null,
  country varchar(255) not null,
  creation_time timestamp not null,
  text varchar(1024) not null,
  primary key (id));