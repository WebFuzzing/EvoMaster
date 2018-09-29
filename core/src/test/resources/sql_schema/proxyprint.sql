create table admin (
  balance_currency varchar(255) not null,
  balance_fractional_part int4 not null,
  balance_integer_part int4 not null,
  email varchar(255),
  id int8 not null,
  primary key (id));

create table consumers (
  balance_currency varchar(255) not null,
  balance_fractional_part int4 not null,
  balance_integer_part int4 not null,
  email varchar(255),
  latitude varchar(255),
  longitude varchar(255),
  name varchar(255) not null,
  id int8 not null,
  primary key (id));

create table documents (
  id  bigserial not null,
  file_name varchar(255) not null,
  total_pages int4 not null,
  print_request_id int8,
  primary key (id));

create table documents_specs (
  id  bigserial not null,
  first_page int4 not null,
  last_page int4 not null,
  printing_schema int8,
  document_id int8,
  primary key (id));

create table employees (
  name varchar(255) not null,
  id int8 not null,
  printshop_id int8,
  primary key (id));

create table managers (
  email varchar(255) not null,
  name varchar(255) not null,
  id int8 not null,
  printshop_id int8,
  primary key (id));

create table notification (
  id  bigserial not null,
  email varchar(255),
  read boolean,
  timestamp timestamp,
  consumer int8,
  primary key (id));

create table pricetables (
  printshop_id int8 not null,
  price float4,
  item varchar(255) not null,
  primary key (printshop_id, item));

create table print_requests (
  id  bigserial not null,
  arrival timestamp,
  cost float8,
  delivered timestamp,
  empattended varchar(255),
  empdelivered varchar(255),
  finished timestamp,
  paypal_sale_id varchar(255),
  payment_type varchar(255),
  status varchar(255),
  consumer_id int8,
  printshop_id int8,
  printshop int8,
  consumer int8,
  primary key (id));

create table printing_schemas (
  id  bigserial not null,
  binding_specs varchar(255),
  cover_specs varchar(255),
  is_deleted boolean not null,
  pschema_name varchar(255) not null,
  paper_specs varchar(255) not null,
  consumer_id int8,
  primary key (id));

create table printshops (
  id  bigserial not null,
  address varchar(255) not null,
  avg_rating float4 not null,
  balance_currency varchar(255) not null,
  balance_fractional_part int4 not null,
  balance_integer_part int4 not null,
  latitude float8 not null,
  logo varchar(255) not null,
  longitude float8 not null,
  name varchar(255) not null,
  nif varchar(255) not null,
  primary key (id));

create table register_requests (
  id  bigserial not null,
  accepted boolean not null,
  manager_email varchar(255) not null,
  manager_name varchar(255) not null,
  manager_password varchar(255) not null,
  manager_username varchar(255) not null,
  pshop_address varchar(255) not null,
  pshop_date_request varchar(255) not null,
  pshop_date_request_accepted varchar(255),
  pshop_latitude float8 not null,
  pshop_longitude float8 not null,
  pshop_nif varchar(255) not null,
  pshop_name varchar(255) not null,
  primary key (id));

create table reviews (
  id  bigserial not null,
  description varchar(255) not null,
  rating int4 not null,
  consumer_id int8,
  printshop int8,
  primary key (id));

create table roles (
  user_id int8 not null,
  roles varchar(255));

create table users (
  id  bigserial not null,
  password varchar(255) not null,
  username varchar(255) not null,
  primary key (id));

alter table users
  add constraint UK_r43af9ap4edm43mmtq01oddj6 unique (username);

alter table admin
  add constraint FKqer4e53tfnl17s22ior7fcsv8 foreign key (id) references users;

alter table consumers
  add constraint FKp8jqyx24t09fa5ypwi2bhpfjf foreign key (id) references users;

alter table documents
  add constraint FKci98yrypvrejxgwa5t6m5e9tb foreign key (print_request_id) references print_requests;

alter table documents_specs
  add constraint FKm0tgx5noftbbrok6ypl3aa5ni foreign key (printing_schema) references printing_schemas;

alter table documents_specs
  add constraint FKe16ibyty3bc1qralb53qe3k3k foreign key (document_id) references documents;

alter table employees
  add constraint FKqqo4nte92snqb19ral9h73i9k foreign key (printshop_id) references printshops;

alter table employees
  add constraint FKd6th9xowehhf1kmmq1dsseq28 foreign key (id) references users;

alter table managers
  add constraint FKnp0x56bv2pdr8a7ri3x3lbqqk foreign key (printshop_id) references printshops;

alter table managers
  add constraint FKo602exy2392s7gi9as93mio60 foreign key (id) references users;

alter table notification
  add constraint FKcnlmmimbh0pa7v4u5a0cidft3 foreign key (consumer) references consumers;

alter table pricetables
  add constraint FKddfhs08jh7xl7nghjw6m7ppmi foreign key (printshop_id) references printshops;

alter table print_requests
  add constraint FKnfccjos4qlij3s0agfyb9f1di foreign key (consumer_id) references consumers;

alter table print_requests
  add constraint FKdqi5u8eu9o85vats3x5yq3hg3 foreign key (printshop_id) references printshops;

alter table print_requests
  add constraint FK37sxxmaokn6bdhahiklpu3x1n foreign key (printshop) references printshops;

alter table print_requests
  add constraint FKihcinqf14qe7o04gf4f2tpert foreign key (consumer) references consumers;

alter table printing_schemas
  add constraint FK4p4dkli4jvxa5n451iibpephq foreign key (consumer_id) references consumers;

alter table reviews
  add constraint FK69ckh7ye2d2lhh20wbe9dt4k2 foreign key (consumer_id) references consumers;

alter table reviews
  add constraint FKb9bjxomo9rd1x8nyp0mhwbfqf foreign key (printshop) references printshops;

alter table roles
  add constraint FK97mxvrajhkq19dmvboprimeg1 foreign key (user_id) references users;
