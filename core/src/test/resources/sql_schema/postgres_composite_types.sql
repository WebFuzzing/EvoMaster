CREATE DOMAIN contact_name AS
   VARCHAR NOT NULL CHECK (value !~ '\s');

CREATE TYPE communication_channels AS ENUM
   ('text_message',
    'email',
    'phone_call',
    'broadcast');

CREATE TYPE complex AS
(
    r double precision,
    i double precision
);

CREATE TYPE inventory_item AS
(
    address        text,
    supplier_id integer,
    price       money
);

CREATE TYPE nested_composite_type AS
(
    item  inventory_item,
    count integer
);

CREATE TABLE on_hand (
   item             inventory_item,
   count            integer,
   nestedColumn     nested_composite_type
);
