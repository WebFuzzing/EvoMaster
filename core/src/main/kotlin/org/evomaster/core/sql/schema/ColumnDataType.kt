package org.evomaster.core.sql.schema

/**
 * SQL Data types from databases
 * See http://www.h2database.com/html/datatypes.html
 * and https://www.postgresql.org/docs/14/datatype.html
 * and https://dev.mysql.com/doc/refman/8.0/en/data-types.html
 */
enum class ColumnDataType(val shouldBePrintedInQuotes: Boolean = false) {


    /**
     * TODO
     * String - set
     * https://dev.mysql.com/doc/refman/8.0/en/set.html
     */
    SET,

    /**
     * date time type
     * https://dev.mysql.com/doc/refman/8.0/en/date-and-time-type-syntax.html
     */
    DATETIME,
    TIME,


    /**
     * year (1 or 2) or 4
     * https://dev.mysql.com/doc/refman/8.0/en/year.html
     */
    YEAR(),

    /**
     * enum type
     * https://dev.mysql.com/doc/refman/8.0/en/enum.html
     */
    ENUM,

    /**
     * bit type
     * https://dev.mysql.com/doc/refman/8.0/en/bit-type.html
     */
    BIT,

    /**
     * https://www.postgresql.org/docs/14/datatype-bit.html
     */
    VARBIT,

    /**
     * A Boolean value (true/false)
     */
    BOOL,
    BOOLEAN,
    TINYINT,

    /**
     * A string value.
     * The length of a CHAR column is fixed.
     */
    CHAR(shouldBePrintedInQuotes = true),


    /**
     * A normal-size integer.
     * The signed range is -2147483648 to 2147483647.
     * The unsigned range is 0 to 4294967295.
     */
    INTEGER,
    INT,
    INT4,

    /**
     * A large integer.
     * The signed range is -9223372036854775808 to 9223372036854775807.
     * The unsigned range is 0 to 18446744073709551615.
     */
    BIGINT,
    INT8,

    /**
     * A string value.
     * The length of the column is variable
     */
    VARCHAR(shouldBePrintedInQuotes = true),


    /**
     * LONG or LONG VARCHAR
     * https://dev.mysql.com/doc/refman/8.0/en/blob.html
     */
    MEDIUMTEXT,
    LONGBLOB,
    TINYBLOB,
    MEDIUMBLOB,
    LONGTEXT,
    TINYTEXT,

    /**
     * The TIMESTAMP data type is used for values that contain both date and time parts.
     * TIMESTAMP has a range of '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
     */
    TIMESTAMP(shouldBePrintedInQuotes = true),

    /**
     * The timestamptz data is the timestamp with time zone. The timestamptz is a time zone-aware date and time data type.
     * The SQL standard requires that writing just timestamp be equivalent to timestamp without time zone, and PostgreSQL
     * honors that behavior. (Releases prior to 7.3 treated it as timestamp with time zone.) timestamptz is accepted as
     * an abbreviation for timestamp with time zone; this is a PostgreSQL extension.
     */
    TIMESTAMPTZ(shouldBePrintedInQuotes = true),


    /**
     * Alias for time with time zone. It is a PostgreSQL extension.
     */
    TIMETZ,


    /**
     * VARBINARY is similar to VARCHAR, except that it contains binary strings rather than nonbinary strings.
     * That is, it contains byte sequences rather than character sequences.
     */
    VARBINARY,


    /**
     * https://dev.mysql.com/doc/refman/8.0/en/binary-varbinary.html
     */
    BINARY,

    /**
     * The FLOAT type represents approximates numeric data values.
     * https://dev.mysql.com/doc/refman/8.0/en/floating-point-types.html
     * MySQL also supports this optional precision specification, but the precision
     * value in FLOAT(p) is used only to determine storage size.
     * A precision from 0 to 23 results in a 4-byte single-precision FLOAT column.
     * A precision from 24 to 53 results in an 8-byte double-precision DOUBLE column.
     */
    FLOAT,

    /**
     *  The DOUBLE type represents approximate numeric data values.
     *  MySQL uses eight bytes for double-precision values.
     */
    DOUBLE,


    /**
     * A 16-bit (2 bytes) exact integer value
     */
    SMALLINT,
    MEDIUMINT,
    INT2,

    /**
     * A CLOB (character large object) value can be up to 2,147,483,647 characters long.
     * A CLOB is used to store unicode character-based data, such as large documents in any character set.
     * The length is given in number characters for both CLOB, unless one of the suffixes K, M, or G is given, relating to the multiples of 1024, 1024*1024, 1024*1024*1024 respectively.
     **/
    CLOB,

    /**
     * Real data can hold a value 4 bytes in size, meaning it has 7 digits of precision
     * (the number of digits to the right of the decimal point).
     * It is also a floating-point numeric that is identical to the floating point statement float(24).
     */
    REAL,

    /**
     * Data type with fixed precision and scale. This data type is recommended for storing currency values.
     * Mapped to java.math.BigDecimal.
     * Example: DECIMAL(20, 2)
     **/
    DECIMAL,
    DEC,

    /**
     * Same as DECIMAL
     */
    NUMERIC,

    /**
     * A Binary Large Object, typically images, audio or multimedia.
     */
    BLOB,


    /**
     * Postgres. The data type uuid stores Universally Unique Identifiers (UUID)
     * as defined by RFC 4122, ISO/IEC 9834-8:2005, and related standards.
     */
    UUID(shouldBePrintedInQuotes = true),

    /**
     * Postgres. In addition, PostgreSQL provides the text type, which stores strings
     * of any length. Although the type text is not in the SQL standard,
     * several other SQL database management systems have it as well.
     * Both TEXT and VARCHAR have the upper limit at 1 GB
     */
    TEXT(shouldBePrintedInQuotes = true),

    /**
     * Postgres. The xml data type can be used to store XML data. Its advantage over
     * storing XML data in a text field is that it checks the input values for well-formedness,
     * and there are support functions to perform type-safe operations on it
     */
    XML,

    /**
     * date (no time of day) minvalue = 4713 BC, maxvalue= 5874897 AD
     */
    DATE,

    JSON,
    JSONB,

    /**
     * BigSerial used as auto-incremental ID.
     * The data types serial and bigserial are not true types, but merely a notational convenience for creating unique
     * identifier columns (similar to the AUTO_INCREMENT property supported by some other databases).
     */
    BIGSERIAL,

    SERIAL,

    /**
     * http://www.h2database.com/html/datatypes.html#timestamp_with_time_zone_type
     */
    TIMESTAMP_WITH_TIME_ZONE,

    /**
     * http://www.h2database.com/html/datatypes.html#time_with_time_zone_type
     */
    TIME_WITH_TIME_ZONE,

    /**
     * https://www.h2database.com/html/datatypes.html#binary_varying_type
     */
    BINARY_VARYING,

    /**
     * https://www.h2database.com/html/datatypes.html#double_precision_type
     */
    DOUBLE_PRECISION,

    /**
     * https://www.h2database.com/html/datatypes.html#binary_large_object_type
     */
    BINARY_LARGE_OBJECT,

    /**
     * https://www.h2database.com/html/datatypes.html#character_large_object_type
     */
    CHARACTER_LARGE_OBJECT(shouldBePrintedInQuotes = true),

    /**
     * https://www.h2database.com/html/datatypes.html#character_type
     */
    CHARACTER(shouldBePrintedInQuotes = true),

    /**
     * https://www.h2database.com/html/datatypes.html#character_varying_type
     */
    CHARACTER_VARYING(shouldBePrintedInQuotes = true),

    /**
     * https://www.h2database.com/html/datatypes.html#varchar_ignorecase_type
     */
    VARCHAR_IGNORECASE(shouldBePrintedInQuotes = true),

    /**
     * https://www.h2database.com/html/datatypes.html#java_object_type
     */
    JAVA_OBJECT,

    /**
     * https://www.h2database.com/html/datatypes.html#geometry_type
     */
    GEOMETRYCOLLECTION,

    /**
     *  https://www.postgresql.org/docs/14/datatype-numeric.html
     */
    FLOAT4,
    FLOAT8,
    SMALLSERIAL,

    /**
     * https://www.postgresql.org/docs/14/datatype-money.html
     */
    MONEY,

    /**
     * https://www.postgresql.org/docs/current/typeconv-query.html
     * The bpchar column type stands for blank-padded char
     */
    BPCHAR,

    /**
     * https://www.postgresql.org/docs/14/datatype-binary.html
     */
    BYTEA,

    /**
     * https://www.postgresql.org/docs/14/datatype-datetime.html
     */
    INTERVAL(shouldBePrintedInQuotes = true),

    /**
     * https://www.postgresql.org/docs/14/datatype-geometric.html
     */
    POINT,
    LINE(shouldBePrintedInQuotes = true),
    LSEG(shouldBePrintedInQuotes = true),
    BOX(shouldBePrintedInQuotes = true),
    PATH(shouldBePrintedInQuotes = true),
    POLYGON(shouldBePrintedInQuotes = true),
    CIRCLE(shouldBePrintedInQuotes = true),

    /**
     * https://dev.mysql.com/doc/refman/8.0/en/spatial-types.html
     * https://h2database.com/html/datatypes.html#geometry_type
     */
    LINESTRING(shouldBePrintedInQuotes = true),
    MULTIPOINT(shouldBePrintedInQuotes = true),
    MULTILINESTRING(shouldBePrintedInQuotes = true),
    MULTIPOLYGON(shouldBePrintedInQuotes = true),
    GEOMETRY(shouldBePrintedInQuotes = true),
    GEOMCOLLECTION(shouldBePrintedInQuotes = true),

    /**
     * https://www.postgresql.org/docs/14/datatype-net-types.html
     */
    CIDR,
    INET,
    MACADDR,
    MACADDR8,

    /**
     * https://www.postgresql.org/docs/14/datatype-textsearch.html
     */
    TSVECTOR,
    TSQUERY,

    /**
     * https://www.postgresql.org/docs/14/datatype-json.html#DATATYPE-JSONPATH
     */
    JSONPATH,

    /**
     * https://www.postgresql.org/docs/14/rangetypes.html
     * built-in range types
     */
    INT4RANGE,
    INT8RANGE,
    NUMRANGE,
    TSRANGE,
    TSTZRANGE,
    DATERANGE,

    /**
     * https://www.postgresql.org/docs/14/rangetypes.html
     * built-in multirange types
     */
    INT4MULTIRANGE,
    INT8MULTIRANGE,
    NUMMULTIRANGE,
    TSMULTIRANGE,
    TSTZMULTIRANGE,
    DATEMULTIRANGE,

    /**
     * https://www.postgresql.org/docs/current/datatype-pg-lsn.html
     * postgres log sequence number
     */
    PG_LSN,

    /**
     * https://www.postgresql.org/docs/current/datatype-oid.html
     * postgres aliases for object identifiers
     */
    OID,
    REGCLASS,
    REGCOLLATION,
    REGCONFIG,
    REGDICTIONARY,
    REGNAMESPACE,
    REGOPER,
    REGOPERATOR,
    REGPROC,
    REGPROCEDURE,
    REGROLE,
    REGTYPE,

    /**
     * This is not an actual built-in column data type,
     * but a placeholder for user-defined composite types.
     */
    COMPOSITE_TYPE;
}
