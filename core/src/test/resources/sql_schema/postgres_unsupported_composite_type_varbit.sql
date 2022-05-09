
CREATE TYPE inventory_item AS
(
    bitcolumn        varbit(15),
    supplier_id integer,
    price       money
);

