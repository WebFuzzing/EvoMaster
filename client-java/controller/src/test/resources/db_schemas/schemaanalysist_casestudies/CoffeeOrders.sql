DROP TABLE order_items;

DROP TABLE orders;

DROP TABLE coffees;

DROP TABLE salespeople;

DROP TABLE customers;

CREATE TABLE coffees (
  id INTEGER PRIMARY KEY,
  coffee_name VARCHAR(50) NOT NULL,
  price INT NOT NULL
);

CREATE TABLE salespeople (
  id INTEGER PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL,
  commission_rate INT NOT NULL
);

CREATE TABLE customers (
  id INTEGER PRIMARY KEY,
  company_name VARCHAR(50) NOT NULL,
  street_address VARCHAR(50) NOT NULL,
  city VARCHAR(50) NOT NULL,
  state VARCHAR(50) NOT NULL,
  zip VARCHAR(50) NOT NULL
);

CREATE TABLE orders (
  id INTEGER PRIMARY KEY,
  customer_id INTEGER,
  salesperson_id INTEGER,
  FOREIGN KEY(customer_id) REFERENCES customers(id),
  FOREIGN KEY(salesperson_id) REFERENCES salespeople(id)
);

CREATE TABLE order_items (
  id INTEGER PRIMARY KEY,
  order_id INTEGER,
  product_id INTEGER,
  product_quantity INTEGER,
  FOREIGN KEY(order_id) REFERENCES orders(id),
  FOREIGN KEY(product_id) REFERENCES coffees(id)
);