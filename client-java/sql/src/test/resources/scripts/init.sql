create table customers
(
    id   int         not null,
    name varchar(25) not null,
    age  int         null,
    constraint customers_pk primary key (id)
);

create table orders
(
    id        int         not null,
    client_id int         not null,
    product   varchar(25) not null,
    constraint orders_pk primary key (id),
    constraint orders_customers_id_fk foreign key (client_id) references customers (id)
);

create table employees
(
    id          int         not null,
    name        varchar(25) not null,
    married     tinyint     null,
    hired       datetime    null,
    constraint employees_pk primary key (id)
);