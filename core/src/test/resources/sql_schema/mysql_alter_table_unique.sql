CREATE TABLE suppliers (
           supplier_id INT AUTO_INCREMENT,
           name VARCHAR(255) NOT NULL,
           phone VARCHAR(15) NOT NULL UNIQUE,
           address VARCHAR(255) NOT NULL,
           PRIMARY KEY (supplier_id)
);

alter table suppliers add constraint uc_name_address UNIQUE (name , address);

alter table suppliers add constraint phone_unique UNIQUE (phone);