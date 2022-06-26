-- https://app.box.com/s/ihlrir6u340uzjjyfzsd
CREATE TABLE 'Divisions' ('DivisionId' INTEGER, 'Name' TEXT);
CREATE TABLE 'Products' ('ProductId' INTEGER, 'SKU' TEXT, 'ProductDescription' TEXT, 'Price' TEXT, 'QuantityInStock' INTEGER);
CREATE TABLE 'ProductsSales' ('SellerId' INTEGER, 'ProductId' INTEGER, 'QuantitySold' INTEGER, 'UnitSalesPrice' TEXT, 'DateOfSale' DATETIME);
CREATE TABLE 'SalesPerformance' ('SellerId' INTEGER, 'TotalSales' TEXT, 'Commission' TEXT);
CREATE TABLE 'Salesmen' ('SalesmanId' INTEGER, 'Division' INTEGER, 'FirstName' TEXT, 'LastName' TEXT, 'SS#' TEXT);
CREATE TABLE 'Switchboard Items' ('SwitchboardID' INTEGER, 'ItemNumber' INTEGER, 'ItemText' TEXT, 'Command' INTEGER, 'Argument' TEXT);
CREATE INDEX 'Divisions_DivisionId' ON 'Divisions' ('DivisionId' );
CREATE UNIQUE INDEX 'Divisions_PrimaryKey' ON 'Divisions' ('DivisionId' );
CREATE UNIQUE INDEX 'ProductsSales_ProductsProductsSales' ON 'ProductsSales' ('SellerId' , 'ProductId' );
CREATE INDEX 'ProductsSales_SalesmenProductsSales' ON 'ProductsSales' ('ProductId' );
CREATE INDEX 'ProductsSales_SellerId' ON 'ProductsSales' ('SellerId' );
CREATE UNIQUE INDEX 'Products_PrimaryKey' ON 'Products' ('ProductId' );
CREATE INDEX 'Products_ProductId' ON 'Products' ('ProductId' );
CREATE INDEX 'SalesPerformance_SellerId' ON 'SalesPerformance' ('SellerId' );
CREATE UNIQUE INDEX 'Salesmen_PrimaryKey' ON 'Salesmen' ('SalesmanId' );
CREATE INDEX 'Salesmen_SalesmanId' ON 'Salesmen' ('SalesmanId' );
CREATE UNIQUE INDEX 'Switchboard Items_PrimaryKey' ON 'Switchboard Items' ('SwitchboardID' , 'ItemNumber' );

