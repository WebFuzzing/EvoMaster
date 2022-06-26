-- http://dhdurso.org/ms-access-downloads.html

CREATE TABLE "Employees" ("EmpNo" TEXT, "fName" TEXT, "lName" TEXT, "Rate" TEXT, "MgrNo" TEXT);
CREATE TABLE "Labor" ("EmpNo" TEXT, "Wono" TEXT, "Start" DATETIME, "End" DATETIME, "Hours" REAL);
CREATE TABLE "Reporting_Relationships" ("EmpNo" TEXT, "MgrNo" TEXT, "Reporting_Relationship" TEXT);
CREATE TABLE "Work_Orders" ("Wono" TEXT, "Descr" TEXT, "Std" REAL, "Accum" REAL);
CREATE UNIQUE INDEX "Employees_PrimaryKey" ON "Employees" ("EmpNo" );
CREATE UNIQUE INDEX "Reporting_Relationships_PrimaryKey" ON "Reporting_Relationships" ("EmpNo" , "MgrNo" );
CREATE INDEX "Labor_LaborWono" ON "Labor" ("EmpNo" );
CREATE INDEX "Labor_Work_OrdersLabor" ON "Labor" ("Wono" );
CREATE UNIQUE INDEX "Labor_TicketNo" ON "Labor" ("EmpNo" , "Wono" );
CREATE UNIQUE INDEX "Work_Orders_PrimaryKey" ON "Work_Orders" ("Wono" );
