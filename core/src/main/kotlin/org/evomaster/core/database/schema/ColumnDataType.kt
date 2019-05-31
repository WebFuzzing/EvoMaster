package org.evomaster.core.database.schema

/**
 * SQL Data types from databases
 * See http://www.h2database.com/html/datatypes.html
 */
enum class ColumnDataType(dataTypeName: String) {

    /**
     * A Boolean value (true/false)
     */
    BOOLEAN("BOOLEAN"),
    TINYINT("TINYINT"),
    /**
     * A string value.
     * The length of a CHAR column is fixed.
     */
    CHAR("CHAR"),
    /**
     * A normal-size integer.
     * The signed range is -2147483648 to 2147483647.
     * The unsigned range is 0 to 4294967295.
     */
    INTEGER("INTEGER"),
    INT4("INT4"),
    /**
     * A large integer.
     * The signed range is -9223372036854775808 to 9223372036854775807.
     * The unsigned range is 0 to 18446744073709551615.
     */
    BIGINT("BIGINT"),
    INT8("INT8"),
    /**
     * A string value.
     * The length of the column is variable
     */
    VARCHAR("VARCHAR"),
    /**
     * The TIMESTAMP data type is used for values that contain both date and time parts.
     * TIMESTAMP has a range of '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
     */
    TIMESTAMP("TIMESTAMP"),
    /**
     * VARBINARY is similar to VARCHAR, except that it contains binary strings rather than nonbinary strings.
     * That is, it contains byte sequences rather than character sequences.
     */
    VARBINARY("VARBINARY"),
    /**
     *  The DOUBLE type represents approximate numeric data values.
     *  MySQL uses eight bytes for double-precision values.
     */
    DOUBLE("DOUBLE"),
    /**
     * A 16-bit (2 bytes) exact integer value
     */
    SMALLINT("SMALLINT"),
    INT2("INT2"),
    /**
     * A CLOB (character large object) value can be up to 2,147,483,647 characters long.
     * A CLOB is used to store unicode character-based data, such as large documents in any character set.
     * The length is given in number characters for both CLOB, unless one of the suffixes K, M, or G is given, relating to the multiples of 1024, 1024*1024, 1024*1024*1024 respectively.
     **/
    CLOB("CLOB"),

    /**
     * Real data can hold a value 4 bytes in size, meaning it has 7 digits of precision
     * (the number of digits to the right of the decimal point).
     * It is also a floating-point numeric that is identical to the floating point statement float(24).
     */
    REAL("REAL"),
    /**
     * Data type with fixed precision and scale. This data type is recommended for storing currency values.
     * Mapped to java.math.BigDecimal.
     * Example: DECIMAL(20, 2)
     **/
    DECIMAL("DECIMAL"),
    /**
     * A Binary Large Object, typically images, audio or multimedia.
     */
    BLOB("BLOB"),
    /**
     * Postgres. The data type uuid stores Universally Unique Identifiers (UUID)
     * as defined by RFC 4122, ISO/IEC 9834-8:2005, and related standards.
     */
    UUID("UUID"),
    /**
     * Postgres. In addition, PostgreSQL provides the text type, which stores strings
     * of any length. Although the type text is not in the SQL standard,
     * several other SQL database management systems have it as well.
     * Both TEXT and VARCHAR have the upper limit at 1 GB
     */
    TEXT("TEXT"),
    /**
     * Postgres. The xml data type can be used to store XML data. Its advantage over
     * storing XML data in a text field is that it checks the input values for well-formedness,
     * and there are support functions to perform type-safe operations on it
     */
    XML("XML"),
    /**
     * date (no time of day) minvalue = 4713 BC, maxvalue= 5874897 AD
     */
    DATE("DATE"),

    JSONB("JSONB");


    fun shouldBePrintedInQuotes(): Boolean {

        return equals(VARCHAR) || equals(CHAR) || equals(TIMESTAMP) || equals(TEXT)
    }
}