
create table Persons (
  person_id bigint not null,
  primary key (person_id)
);

create table Doctors (
  person_id bigint not null,
  authorization_number bigint not null,
  primary key (person_id)
);

alter table Doctors
  add constraint FKDoctors foreign key (person_id) references Persons;
