DROP TABLE IF EXISTS Products CASCADE;

DROP TABLE IF EXISTS Orders CASCADE;

DROP TABLE IF EXISTS order_items CASCADE;

CREATE TABLE products (
    product_no integer PRIMARY KEY NOT NULL,
    name varchar(100) NOT NULL,
    price numeric NOT NULL,
    CHECK (price > 0),
    discounted_price numeric NOT NULL,
    CHECK (discounted_price > 0),
    CHECK (price > discounted_price)
);

CREATE TABLE orders (
    order_id integer PRIMARY KEY,
    shipping_address varchar(100)
);

CREATE TABLE order_items (
    product_no integer REFERENCES products,
    order_id integer REFERENCES orders,
    quantity integer NOT NULL,
    PRIMARY KEY (product_no, order_id),
    CHECK (quantity > 0)
);