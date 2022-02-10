CREATE TYPE complex AS
(
    r double precision,
    i double precision
);

CREATE TYPE inventory_item AS
(
    name        text,
    supplier_id integer,
    price       numeric
);

CREATE TYPE nested_composite_type AS
(
    item  inventory_item,
    count integer
);

CREATE TABLE on_hand (
   item      inventory_item,
   count     integer
);