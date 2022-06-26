-- http://dhdurso.org/ms-access-downloads.html
CREATE TABLE 'Admissions' ('AdmitNo' INTEGER, 'PatNo' TEXT, 'Diag_Code' TEXT, 'Admit_Date' DATETIME, 'Dischg_Date' DATETIME, 'CoPay' TEXT);
CREATE TABLE 'Diagnostics' ('DiagNo' TEXT, 'Descr' TEXT, 'Cost' TEXT);
CREATE TABLE 'Patients' ('PatNo' TEXT, 'fName' TEXT, 'lName' TEXT, 'BirthDate' DATETIME, 'Address' TEXT, 'City' TEXT, 'State' TEXT, 'Zip' TEXT);
CREATE INDEX 'Admissions_LaborWono' ON 'Admissions' ('PatNo' );
CREATE UNIQUE INDEX 'Admissions_PrimaryKey' ON 'Admissions' ('AdmitNo' );
CREATE INDEX 'Admissions_Work_OrdersLabor' ON 'Admissions' ('Diag_Code' );
CREATE UNIQUE INDEX 'Diagnostics_PrimaryKey' ON 'Diagnostics' ('DiagNo' );
CREATE UNIQUE INDEX 'Patients_PrimaryKey' ON 'Patients' ('PatNo' );
