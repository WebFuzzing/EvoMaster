package org.evomaster.client.java.sql;

/**
 * Enumeration representing various SQL data types.
 * This class provides utilities to handle SQL data types, including conversions
 * and checks for grouping types (e.g., string types, numeric types, date/time types).
 */
public enum SqlDataType {
    // Boolean Types
    BOOL,
    BOOLEAN,

    // Numeric Types
    INT,
    INTEGER,
    INT2,
    INT4,
    SMALLINT,
    TINYINT,
    INT8,
    BIGINT,
    BIGSERIAL,

    // Floating Point Types
    DOUBLE,
    DOUBLE_PRECISION,
    FLOAT,
    REAL,
    FLOAT4,
    FLOAT8,
    DEC,
    DECIMAL,

    // Date and Time Types
    TIMESTAMP,
    TIMESTAMPZ,
    TIMETZ,
    DATE,
    DATETIME,
    TIME,

    // Character and Text Types
    CHAR,
    CHARACTER,
    CHARACTER_LARGE_OBJECT,
    TINYTEXT,
    TEXT,
    LONGTEXT,
    VARCHAR,
    CLOB,
    CHARACTER_VARYING,
    VARCHAR_IGNORECASE,
    MEDIUMTEXT,

    // UUID Type
    UUID,

    // Binary Types
    LONGBLOB,
    MEDIUMBLOB,
    TINYBLOB;


    public static SqlDataType fromString(String input) {
        String normalized = input.toUpperCase().replace(' ', '_');
        return SqlDataType.valueOf(normalized);
    }


    public static boolean isStringType(SqlDataType dataType) {
        switch (dataType) {
            case CHAR:
            case CHARACTER:
            case CHARACTER_LARGE_OBJECT:
            case TINYTEXT:
            case TEXT:
            case LONGTEXT:
            case VARCHAR:
            case CHARACTER_VARYING:
            case VARCHAR_IGNORECASE:
            case CLOB:
            case MEDIUMTEXT:
            case LONGBLOB:
            case MEDIUMBLOB:
            case TINYBLOB:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDateTimeType(SqlDataType dataType) {
        switch (dataType) {
            case TIMESTAMP:
            case TIMESTAMPZ:
            case TIMETZ:
            case DATE:
            case DATETIME:
            case TIME:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDoubleType(SqlDataType dataType) {
        switch (dataType) {
            case DOUBLE:
            case DOUBLE_PRECISION:
            case FLOAT:
            case REAL:
            case FLOAT4:
            case FLOAT8:
            case DEC:
            case DECIMAL:
                return true;
            default:
                return false;
        }
    }

    public static boolean isLongType(SqlDataType dataType) {
        switch (dataType) {
            case INT8:
            case BIGINT:
            case BIGSERIAL:
                return true;
            default:
                return false;
        }
    }

    public static boolean isByteType(SqlDataType dataType) {
        switch (dataType) {
            case TINYINT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isShortType(SqlDataType dataType) {
        switch (dataType) {
            case INT2:
            case SMALLINT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isIntegerType(SqlDataType dataType) {
        switch (dataType) {
            case INT:
            case INTEGER:
            case INT4:
                return true;
            default:
                return false;
        }
    }

    public static boolean isBooleanType(SqlDataType dataType) {
        switch (dataType) {
            case BOOL:
            case BOOLEAN:
                return true;
            default:
                return false;
        }
    }

    public static boolean isUUIDType(SqlDataType dataType) {
        return dataType == SqlDataType.UUID;
    }

}
