CREATE TABLE IntegerTypes (
    smallintColumn smallint NOT NULL,
    integerColumn integer NOT NULL,
    bigintColumn bigint NOT NULL,
    decimalColumn decimal NOT NULL
);

CREATE TABLE ArbitraryPrecisionNumbers (
    numericColumn numeric NOT NULL
);

CREATE TABLE FloatingPointTypes(
    realColumn real NOT NULL,
    doublePrecisionColumn double precision NOT NULL
);

CREATE TABLE SerialTypes(
    smallserialColumn smallserial NOT NULL,
    serialColumn serial NOT NULL,
    bigserialColumn bigserial NOT NULL
);

CREATE TABLE MonetaryTypes(
    moneyColumn money NOT NULL
);

CREATE TABLE CharacterTypes(
    varcharColunmn varchar(10) NOT NULL,
    charColumn char(10) NOT NULL,
    textColumn text NOT NULL
);

CREATE TABLE BinaryDataTypes(
    byteaColumn bytea NOT NULL
);

CREATE TABLE DateTimeTypes(
    timestampColumn timestamp NOT NULL,
    timestampWithTimeZone timestamp with time zone NOT NULL,
    dateColumn date NOT NULL,
    timeColumn time NOT NULL,
    timeWithTimeZoneColumn time with time zone NOT NULL,
    intervalColumn interval NOT NULL
);

CREATE TABLE BooleanType(
    booleanColumn boolean NOT NULL
);

CREATE TABLE GeometricTypes(
    pointColumn point NOT NULL,
    lineColumn line NOT NULL,
    lsegColumn lseg NOT NULL,
    boxColumn box NOT NULL,
    pathColumn path NOT NULL,
    polygonColumn polygon NOT NULL,
    circleColumn circle NOT NULL,
    cidrColumn cidr NOT NULL
);

CREATE TABLE NetworkAddressTypes(
    inetColumn inet NOT NULL,
    macaddrColumn macaddr NOT NULL,
    macaddr8Column macaddr8 NOT NULL
);

CREATE TABLE BitStringTypes(
    bitColumn bit NOT NULL,
    bitVaryingColumn bit varying NOT NULL
);

CREATE TABLE TextSearchTypes(
    tsvectorColumn tsvector NOT NULL,
    tsqueryColumn tsquery NOT NULL
);

CREATE TABLE UUIDType(
    uuidColumn uuid NOT NULL
);

CREATE TABLE XMLType(
    xmlColumn xml NOT NULL
);

CREATE TABLE JSONTypes(
    jsonColumn json NOT NULL,
    jsonbColumn jsonb NOT NULL,
    jsonpathColumn jsonpath NOT NULL
);

CREATE TABLE BuiltInRangeTypes(
    int4rangeColumn int4range NOT NULL,
    int8rangeColumn int8range NOT NULL,
    numrangeColumn numrange NOT NULL,
    tsrangeColumn tsrange NOT NULL,
    tstzrangeColumn tstzrange NOT NULL,
    daterangeColumn daterange NOT NULL
);
