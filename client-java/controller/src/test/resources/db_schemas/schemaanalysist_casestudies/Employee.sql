DROP TABLE Employee;

create table Employee
(
   id INT PRIMARY KEY, 	
   first varchar(15),
   last varchar(20),
   age int,
   address varchar(30),
   city varchar(20),
   state varchar(20)
   CHECK (id >= 0) 
   CHECK (age > 0)	 	
   CHECK (age <= 150)	 	
);
