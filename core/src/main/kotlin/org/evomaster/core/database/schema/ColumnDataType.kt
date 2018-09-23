package org.evomaster.core.database.schema

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
    /**
     * A large integer.
     * The signed range is -9223372036854775808 to 9223372036854775807.
     * The unsigned range is 0 to 18446744073709551615.
     */
    BIGINT("BIGINT"),
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
    /**
     * A CLOB (character large object) value can be up to 2,147,483,647 characters long.
     * A CLOB is used to store unicode character-based data, such as large documents in any character set.
     * The length is given in number characters for both CLOB, unless one of the suffixes K, M, or G is given, relating to the multiples of 1024, 1024*1024, 1024*1024*1024 respectively.
     **/
    CLOB("CLOB")

}