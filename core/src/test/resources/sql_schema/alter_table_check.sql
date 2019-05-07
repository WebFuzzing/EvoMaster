create table People (
	id integer primary key,
	age integer);

alter table People add check (age<=100);

