-- http://www.monetdb.org/Documentation/Guide/Tutorial

CREATE TABLE "voyages" (
	"number"            integer	NOT NULL,
	"number_sup"        char(1)	NOT NULL,
	"trip"              integer,
	"trip_sup"          char(1),
	"boatname"          varchar(50),
	"master"            varchar(50),
	"tonnage"           integer,
	"type_of_boat"      varchar(30),
	"built"             varchar(15),
	"bought"            varchar(15),
	"hired"             varchar(15),
	"yard"              char(1),
	"chamber"           char(1),
	"departure_date"    date,
	"departure_harbour" varchar(30),
	"cape_arrival"      date,
	"cape_departure"    date,
	"cape_call"         boolean,
	"arrival_date"      date,
	"arrival_harbour"   varchar(30),
	"next_voyage"       integer,
	"particulars"       varchar(1285)
);

CREATE TABLE "craftsmen" (
	"number"               integer	NOT NULL,
	"number_sup"           char(1)	NOT NULL,
	"trip"                 integer,
	"trip_sup"             char(1),
	"onboard_at_departure" integer,
	"death_at_cape"        integer,
	"left_at_cape"         integer,
	"onboard_at_cape"      integer,
	"death_during_voyage"  integer,
	"onboard_at_arrival"   integer
);

CREATE TABLE "impotenten" (
	"number"               integer	NOT NULL,
	"number_sup"           char(1)	NOT NULL,
	"trip"                 integer,
	"trip_sup"             char(1),
	"onboard_at_departure" integer,
	"death_at_cape"        integer,
	"left_at_cape"         integer,
	"onboard_at_cape"      integer,
	"death_during_voyage"  integer,
	"onboard_at_arrival"   integer
);

CREATE TABLE "invoices" (
	"number"     integer,
	"number_sup" char(1),
	"trip"       integer,
	"trip_sup"   char(1),
	"invoice"    integer,
	"chamber"    char(1)
);

CREATE TABLE "passengers" (
	"number"               integer	NOT NULL,
	"number_sup"           char(1)	NOT NULL,
	"trip"                 integer,
	"trip_sup"             char(1),
	"onboard_at_departure" integer,
	"death_at_cape"        integer,
	"left_at_cape"         integer,
	"onboard_at_cape"      integer,
	"death_during_voyage"  integer,
	"onboard_at_arrival"   integer
);

CREATE TABLE "seafarers" (
	"number"               integer	NOT NULL,
	"number_sup"           char(1)	NOT NULL,
	"trip"                 integer,
	"trip_sup"             char(1),
	"onboard_at_departure" integer,
	"death_at_cape"        integer,
	"left_at_cape"         integer,
	"onboard_at_cape"      integer,
	"death_during_voyage"  integer,
	"onboard_at_arrival"   integer
);

CREATE TABLE "soldiers" (
	"number"               integer	NOT NULL,
	"number_sup"           char(1)	NOT NULL,
	"trip"                 integer,
	"trip_sup"             char(1),
	"onboard_at_departure" integer,
	"death_at_cape"        integer,
	"left_at_cape"         integer,
	"onboard_at_cape"      integer,
	"death_during_voyage"  integer,
	"onboard_at_arrival"   integer
);

CREATE TABLE "total" (
	"number"               integer	NOT NULL,
	"number_sup"           char(1)	NOT NULL,
	"trip"                 integer,
	"trip_sup"             char(1),
	"onboard_at_departure" integer,
	"death_at_cape"        integer,
	"left_at_cape"         integer,
	"onboard_at_cape"      integer,
	"death_during_voyage"  integer,
	"onboard_at_arrival"   integer
);

ALTER TABLE "voyages" ADD PRIMARY KEY ("number", "number_sup");
ALTER TABLE "craftsmen" ADD PRIMARY KEY ("number", "number_sup");
ALTER TABLE "impotenten" ADD PRIMARY KEY ("number", "number_sup");
ALTER TABLE "passengers" ADD PRIMARY KEY ("number", "number_sup");
ALTER TABLE "seafarers" ADD PRIMARY KEY ("number", "number_sup");
ALTER TABLE "soldiers" ADD PRIMARY KEY ("number", "number_sup");
ALTER TABLE "total" ADD PRIMARY KEY ("number", "number_sup");

ALTER TABLE "craftsmen" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");
ALTER TABLE "impotenten" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");
ALTER TABLE "invoices" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");
ALTER TABLE "passengers" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");
ALTER TABLE "seafarers" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");
ALTER TABLE "soldiers" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");
ALTER TABLE "total" ADD FOREIGN KEY ("number", "number_sup")
	REFERENCES "voyages" ("number", "number_sup");



