create table X (
    id INT,
    constraint pk_x primary key (id)
);

create table Y (
   id INT,
   x INT,
   constraint pk_y primary key (id),
   constraint fk_x foreign key (x) references X (id)
);