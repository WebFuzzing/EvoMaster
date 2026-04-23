CREATE TABLE IntegerTypes (
    dummyColumn integer NOT NULL,

    smallintColumn smallint NOT NULL,
    integerColumn integer NOT NULL,
    bigintColumn bigint NOT NULL,
    decimalColumn decimal NOT NULL
);

CREATE TABLE ArbitraryPrecisionNumbers (
    dummyColumn integer NOT NULL,
    numericColumn numeric NOT NULL
);

CREATE TABLE FloatingPointTypes(
    dummyColumn integer NOT NULL,
    realColumn real NOT NULL,
    doublePrecisionColumn double precision NOT NULL
);

CREATE TABLE SerialTypes(
    dummyColumn integer NOT NULL,
    smallserialColumn smallserial NOT NULL,
    serialColumn serial NOT NULL,
    bigserialColumn bigserial NOT NULL
);

CREATE TABLE MonetaryTypes(
    dummyColumn integer NOT NULL,
    moneyColumn money NOT NULL
);

CREATE TABLE CharacterTypes(
    dummyColumn integer NOT NULL,
    varcharColunmn varchar(10) NOT NULL,
    charColumn char(10) NOT NULL,
    textColumn text NOT NULL
);

CREATE TABLE BinaryDataTypes(
    dummyColumn integer NOT NULL,
    byteaColumn bytea NOT NULL
);

CREATE TABLE DateTimeTypes(
    dummyColumn integer NOT NULL,
    timestampColumn timestamp NOT NULL,
    timestampWithTimeZone timestamp with time zone NOT NULL,
    dateColumn date NOT NULL,
    timeColumn time NOT NULL,
    timeWithTimeZoneColumn time with time zone NOT NULL,
    intervalColumn interval NOT NULL
);

CREATE TABLE BooleanType(
    dummyColumn integer NOT NULL,
    booleanColumn boolean NOT NULL
);

CREATE TABLE GeometricTypes(
    dummyColumn integer NOT NULL,
    pointColumn point NOT NULL,
    lineColumn line NOT NULL,
    lsegColumn lseg NOT NULL,
    boxColumn box NOT NULL,
    pathColumn path NOT NULL,
    polygonColumn polygon NOT NULL,
    circleColumn circle NOT NULL
);

CREATE TABLE NetworkAddressTypes(
    dummyColumn integer NOT NULL,
    cidrColumn cidr NOT NULL,
    inetColumn inet NOT NULL,
    macaddrColumn macaddr NOT NULL,
    macaddr8Column macaddr8 NOT NULL
);

CREATE TABLE BitStringTypes(
    dummyColumn integer NOT NULL,
    bitColumn bit NOT NULL,
    bitVaryingColumn bit varying NOT NULL
);

CREATE TABLE TextSearchTypes(
    dummyColumn integer NOT NULL,
    tsvectorColumn tsvector NOT NULL,
    tsqueryColumn tsquery NOT NULL
);

CREATE TABLE UUIDType(
    dummyColumn integer NOT NULL,
    uuidColumn uuid NOT NULL
);

CREATE TABLE XMLType(
    dummyColumn integer NOT NULL,
    xmlColumn xml NOT NULL
);

CREATE TABLE JSONTypes(
    dummyColumn integer NOT NULL,
    jsonColumn json NOT NULL,
    jsonbColumn jsonb NOT NULL,
    jsonpathColumn jsonpath NOT NULL
);

CREATE TABLE BuiltInRangeTypes(
    dummyColumn integer NOT NULL,
    int4rangeColumn int4range NOT NULL,
    int8rangeColumn int8range NOT NULL,
    numrangeColumn numrange NOT NULL,
    tsrangeColumn tsrange NOT NULL,
    tstzrangeColumn tstzrange NOT NULL,
    daterangeColumn daterange NOT NULL
);

CREATE TYPE EnumType AS ENUM
   ('value0',
    'value1',
    'value2',
    'value3');

CREATE TABLE TableUsingEnumType (
    dummyColumn integer NOT NULL,
    enumColumn EnumType NOT null
);

CREATE TYPE complex AS (
    r double precision,
    i double precision
);

CREATE TABLE TableUsingCompositeType (
    dummyColumn integer,
    complexColumn complex
);


CREATE TYPE inventory_item AS
(
    zipCode        integer,
    supplier_id integer,
    price       money
);

CREATE TYPE nested_composite_type AS
(
    item  inventory_item,
    count integer
);

CREATE TABLE TableUsingNestedCompositeType (
   dummyColumn integer NOT NULL,
   item             inventory_item NOT NULL,
   count            integer NOT NULL,
   nestedColumn     nested_composite_type NOT NULL
);

CREATE TABLE ArrayTypes (
  dummyColumn integer NOT NULL,

  integerArrayColumn integer[] NOT NULL,
  integerMatrixColumn integer[][] NOT NULL,
  integerSpaceColumn integer[][][] NOT NULL,
  integerManyDimensionsColumn integer[][][][] NOT NULL,
  integerExactSizeArrayColumn integer[3] NOT NULL,
  integerExactSizeMatrixColumn integer[3][3] NOT NULL,

  varcharArrayColumn varchar[] NOT NULL,
  varcharmatrixColumn varchar[][] NOT NULL,
  varcharSpaceColumn varchar[][][] NOT NULL,
  varcharManyDimensionsColumn varchar[][][][] NOT NULL,
  varcharExactSizeArrayColumn varchar[3] NOT NULL,
  varcharExactSizeMatrixColumn varchar[3][3] NOT NULL
);

CREATE TABLE PgLsnType (
    dummyColumn integer NOT NULL,

    pglsnColumn pg_lsn NOT NULL
);

CREATE TABLE MultiRangeBuiltInTypes(
    dummyColumn integer NOT NULL,

    int4multirangeColumn int4multirange NOT NULL,
    int8multirangeColumn int8multirange NOT NULL,
    nummultirangeColumn  nummultirange  NOT NULL,
    tsmultirangeColumn tsmultirange NOT NULL,
    tstzmultirangeColumn tstzmultirange NOT NULL,
    datemultirangeColumn datemultirange NOT NULL
);

CREATE TABLE ObjectIdentifierTypes (
    dummyColumn integer NOT NULL,

    oidColumn oid NOT NULL,
    regclassColumn regclass NOT NULL,
    regcollationColumn regcollation NOT NULL,
    regconfigColumn regconfig NOT NULL,
    regdictionaryColumn regdictionary NOT NULL,
    regnamespaceColumn regnamespace NOT NULL,
    regoperColumn regoper NOT NULL,
    regoperatorColumn regoperator NOT NULL,
    regprocColumn regproc NOT NULL,
    regprocedureColumn regprocedure NOT NULL,
    regroleColumn regrole NOT NULL,
    regtypeColumn regtype NOT NULL
);
