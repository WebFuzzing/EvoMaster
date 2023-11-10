create table products
(
    id          int         not null,
    name        varchar(25) not null,
    price       int         not null,
    in_stock    tinyint     not null,
    constraint products_pk primary key (id)
);