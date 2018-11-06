
create table Countries (
  country_id bigint not null,
  country_name varchar (255) not null unique,
  primary key (country_id)
);

create table Passports (
  country_id bigint not null,
  passport_number bigint not null,
  expiration_date timestamp not null,
  birth_date timestamp not null,
  primary key (country_id, passport_number)
);

alter table Passports
  add constraint FKCountries foreign key (country_id) references Countries;

alter table Passports
  add constraint PassportUnique unique (country_id, passport_Number);

alter table Passports
  add constraint ValidPassportNumber check (passport_number>0);

