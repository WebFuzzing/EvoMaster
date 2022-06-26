DROP TABLE Inventory;

CREATE TABLE Inventory
(
   id INT PRIMARY KEY,
   product VARCHAR(50) UNIQUE,
   quantity INT,
   price DECIMAL(18,2)
);