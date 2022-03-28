CREATE TABLE suppliers (
           supplier_id INT AUTO_INCREMENT,
           name VARCHAR(255) NOT NULL,
           phone VARCHAR(15) NOT NULL UNIQUE,
           address VARCHAR(255) NOT NULL,
           PRIMARY KEY (supplier_id),
           CONSTRAINT uc_name_address UNIQUE (name , address)
);