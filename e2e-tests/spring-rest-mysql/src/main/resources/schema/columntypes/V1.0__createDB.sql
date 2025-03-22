CREATE TABLE integerdatatypes (
  integercolumn INTEGER NOT NULL,
  /* TINYINT */
  tinyintcolumn TINYINT NOT NULL,
  tinyintunsignedcolumn TINYINT Unsigned NOT NULL,
  /* SMALLINT */
  smallintcolumn SMALLINT NOT NULL,
  smallintunsignedcolumn SMALLINT UNSIGNED NOT NULL,
  /* MEDIUMINT */
  mediumintcolum MEDIUMINT NOT NULL,
  mediumintunsignedcolum MEDIUMINT UNSIGNED NOT NULL,
  /* INT */
  intcolumn INT NOT NULL,
  intunsignedcolumn INT UNSIGNED NOT NULL,
  /* BIGINT */
  bigintcolumn BIGINT NOT NULL,
  bigintunsignedcolumn BIGINT Unsigned NOT NULL,
  /* INTEGER UNSIGNED */
  integerunsignedcolumn INTEGER UNSIGNED NOT NULL
);

CREATE TABLE fixedpointtypes (
  integercolumn INTEGER NOT NULL,

  decimalcolumn DECIMAL(5) NOT NULL,
  /*UNSIGNED DECIMAL, UNSIGNED DEC, UNSIGNED NUMERIC, UNSIGNED FIXED are deprecated */
  deccolumn DEC(5) NOT NULL,
  numericcolumn NUMERIC(5) NOT NULL,
  fixedcolumn FIXED(5) NOT NULL
);

CREATE TABLE floatingpointtypes (
  integercolumn INTEGER NOT NULL,

  /*FLOAT(M,D), DOUBLE(M,D) are deprecated */
  floatcolumn FLOAT NOT NULL,
  doublecolumn DOUBLE NOT NULL,
  doubleprecisioncolumn DOUBLE PRECISION NOT NULL,
  realcolumn REAL NOT NULL
);

CREATE TABLE bitdatatype (
  integercolumn INTEGER NOT NULL,

  bitcolumn BIT NOT NULL
);

CREATE TABLE booleandatatypes (
  integercolumn INTEGER NOT NULL,

  boolcolumn BOOL NOT NULL,
  booleancolumn BOOLEAN NOT NULL
);

CREATE TABLE serialdatatype (
  integercolumn INTEGER NOT NULL,

  serialcolumn SERIAL NOT NULL
);

CREATE TABLE dateandtimetypes (
  integercolumn INTEGER NOT NULL,

  datecolumn DATE NOT NULL,
  timecolumn TIME NOT NULL,
  datetimecolumn DATETIME NOT NULL,
  timestampcolumn TIMESTAMP NOT NULL,
  yearcolumn YEAR NOT NULL
);

CREATE TABLE stringdatatypes (
  integercolumn INTEGER NOT NULL,

  charcolumn CHAR(4) NOT NULL,
  varcharcolumn VARCHAR(4) NOT NULL,
  binarycolumn BINARY(5) NOT NULL,
  varbinarycolumn VARBINARY(5) NOT NULL,
  tinyblobcolumn TINYBLOB NOT NULL,
  blobcolumn BLOB NOT NULL,
  mediumblobcolumn MEDIUMBLOB NOT NULL,
  longblobcolumn LONGBLOB NOT NULL,
  tinytextcolumn TINYTEXT NOT NULL,
  textcolumn TEXT NOT NULL,
  mediumtextcolumn MEDIUMTEXT NOT NULL,
  longtextcolumn LONGTEXT NOT NULL
);

CREATE TABLE jsondatatypes (
  integercolumn INTEGER NOT NULL,

  jsoncolumn JSON NOT NULL
);

CREATE TABLE spatialdatatypes (
  integercolumn INTEGER NOT NULL,

  pointcolumn POINT NOT NULL,
  linestringcolumn LINESTRING NOT NULL,
  polygoncolumn POLYGON NOT NULL,
  multipointcolumn MULTIPOINT NOT NULL,
  multilinestringcolumn MULTILINESTRING NOT NULL,
  multipolygoncolumn MULTIPOLYGON NOT NULL,
  geometrycolumn GEOMETRY NOT NULL,
  geometrycollectioncolumn GEOMETRYCOLLECTION NOT NULL
);


