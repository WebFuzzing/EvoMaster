create table People (
	id integer primary key,
	age integer check (age<=100)
	);