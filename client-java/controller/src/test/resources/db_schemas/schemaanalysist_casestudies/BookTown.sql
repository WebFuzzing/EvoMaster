DROP TABLE IF EXISTS books CASCADE;

DROP TABLE IF EXISTS publishers CASCADE;

DROP TABLE IF EXISTS authors CASCADE;

DROP TABLE IF EXISTS states CASCADE;

DROP TABLE IF EXISTS my_list CASCADE;

DROP TABLE IF EXISTS stock CASCADE;

DROP SEQUENCE IF EXISTS subject_ids CASCADE;

DROP SEQUENCE IF EXISTS author_ids CASCADE;

DROP TABLE IF EXISTS numeric_values CASCADE;

DROP TABLE IF EXISTS daily_inventory CASCADE;

DROP TABLE IF EXISTS money_example CASCADE;

DROP TABLE IF EXISTS shipments CASCADE;

DROP TABLE IF EXISTS customers CASCADE;

DROP TABLE IF EXISTS book_queue CASCADE;

DROP TABLE IF EXISTS stock_backup CASCADE;

DROP TABLE IF EXISTS favorite_books CASCADE;

DROP TABLE IF EXISTS employees CASCADE;

DROP TABLE IF EXISTS editions CASCADE;

DROP TABLE IF EXISTS distinguished_authors CASCADE;

DROP TABLE IF EXISTS favorite_authors CASCADE;

DROP TABLE IF EXISTS "varchar(100)_sorting" CASCADE;

DROP TABLE IF EXISTS subjects CASCADE;

DROP TABLE IF EXISTS alternate_stock CASCADE;

DROP TABLE IF EXISTS book_backup CASCADE;

DROP TABLE IF EXISTS schedules CASCADE;

DROP SEQUENCE IF EXISTS "shipments_ship_id_seq";

DROP SEQUENCE IF EXISTS "book_ids";

CREATE TABLE "books" (
	"id" integer NOT NULL,
	"title" varchar(100) NOT NULL,
	"author_id" integer,
	"subject_id" integer,
	Constraint "books_id_pkey" Primary Key ("id")
);

CREATE TABLE "publishers" (
	"id" integer NOT NULL,
	"name" varchar(100),
	"address" varchar(100),
	Constraint "publishers_pkey" Primary Key ("id")
);

CREATE TABLE "authors" (
	"id" integer NOT NULL,
	"last_name" varchar(100),
	"first_name" varchar(100),
	Constraint "authors_pkey" Primary Key ("id")
);

CREATE TABLE "states" (
	"id" integer NOT NULL,
	"name" varchar(100),
	"abbreviation" character(2),
	Constraint "state_pkey" Primary Key ("id")
);

CREATE TABLE "my_list" (
	"todos" varchar(100)
);

CREATE TABLE "stock" (
	"isbn" varchar(100) NOT NULL,
	"cost" numeric(5,2),
	"retail" numeric(5,2),
	"stock" integer,
	Constraint "stock_pkey" Primary Key ("isbn")
);

CREATE SEQUENCE "subject_ids" start 0 increment 1 maxvalue 2147483647 minvalue 0  cache 1 ;

CREATE TABLE "numeric_values" (
	"num" numeric(30,6)
);

CREATE TABLE "daily_inventory" (
	"isbn" varchar(100),
	"is_stocked" boolean
);

CREATE TABLE "money_example" (
	"money_cash" numeric(6,2),
	"numeric_cash" numeric(6,2)
);

CREATE TABLE "shipments" (
	"id" integer DEFAULT nextval('"shipments_ship_id_seq"'::varchar(100)) NOT NULL,
	"customer_id" integer,
	"isbn" varchar(100),
	"ship_date" timestamp --with time zone
);

CREATE TABLE "customers" (
	"id" integer NOT NULL,
	"last_name" varchar(100),
	"first_name" varchar(100),
	Constraint "customers_pkey" Primary Key ("id")
);

CREATE SEQUENCE "book_ids" start 0 increment 1 maxvalue 2147483647 minvalue 0  cache 1 ;

CREATE TABLE "book_queue" (
	"title" varchar(100) NOT NULL,
	"author_id" integer,
	"subject_id" integer,
	"approved" boolean
);

CREATE TABLE "stock_backup" (
	"isbn" varchar(100),
	"cost" numeric(5,2),
	"retail" numeric(5,2),
	"stock" integer
);

CREATE TABLE "favorite_books" (
	"employee_id" int,
	"books" varchar(100)
);

CREATE SEQUENCE "shipments_ship_id_seq" start 0 increment 1 maxvalue 2147483647 minvalue 0  cache 1 ;

CREATE TABLE "employees" (
	"id" int PRIMARY KEY NOT NULL,
	"last_name" varchar(100) NOT NULL,
	"first_name" varchar(100),
	CONSTRAINT "employees_id" CHECK ((id > 100))
);

CREATE TABLE "editions" (
	"isbn" varchar(100) NOT NULL PRIMARY KEY,
	"book_id" integer,
	"edition" integer,
	"publisher_id" integer,
	"publication" date,
	"type" character(1),
	CONSTRAINT "integrity" CHECK (((book_id NOTNULL) AND (edition NOTNULL)))
);

CREATE SEQUENCE "author_ids" start 0 increment 1 maxvalue 2147483647 minvalue 0  cache 1 ;

CREATE TABLE "distinguished_authors" (
	"id" integer NOT NULL,
	"last_name" varchar(100),
	"first_name" varchar(100),
	"award" varchar(100),
	Constraint "distinguished_authors_pkey" Primary Key ("id")
)

CREATE TABLE "favorite_authors" (
	"employee_id" int,
	"authors_and_titles" varchar(100)
);

CREATE TABLE "varchar(100)_sorting" (
	"letter" character(1)
);

CREATE TABLE "subjects" (
	"id" integer NOT NULL,
	"subject" varchar(100),
	"location" varchar(100),
	Constraint "subjects_pkey" Primary Key ("id")
);

CREATE TABLE "alternate_stock" (
	"isbn" varchar(100),
	"cost" numeric(5,2),
	"retail" numeric(5,2),
	"stock" integer
);

CREATE TABLE "book_backup" (
	"id" integer,
	"title" varchar(100),
	"author_id" integer,
	"subject_id" integer
);

CREATE TABLE "schedules" (
        "employee_id" integer NOT NULL,
        "schedule" varchar(100),
        Constraint "schedules_pkey" Primary Key ("employee_id")
);


