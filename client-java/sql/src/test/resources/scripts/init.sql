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

create table payments
(
    id          int         not null,
    order_id    int         not null,
    amount      int         not null,
    created     datetime    not null,
    approved    tinyint     not null,
    constraint orders_pk primary key (id),
    constraint payment_orders_id_fk foreign key (order_id) references orders (id)
);