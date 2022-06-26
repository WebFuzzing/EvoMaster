DROP TABLE S;

DROP TABLE T;

CREATE TABLE T 
(
     A int, B int, C int,
     CONSTRAINT UniqueOnColsAandB UNIQUE (A, B)
);

CREATE TABLE S 
(
     X int, Y int, Z int,
     CONSTRAINT RefToColsAandB FOREIGN KEY (X, Y)
     REFERENCES T (A, B)
);
