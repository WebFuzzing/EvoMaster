CREATE TABLE UserInfo 
(
	card_number int PRIMARY KEY, 
	pin_number int not null, 
	user_name varchar(50) not null, 
	acct_lock int
);

CREATE TABLE Account 
(
	id int PRIMARY KEY, 
	account_name varchar(50) not null, 
	user_name varchar(50) not null, 
	balance int default 0, 
	card_number int not null, 
	foreign key(card_number) references UserInfo(card_number)
);