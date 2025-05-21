package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.SqlDataType;

import java.util.Objects;

import static org.evomaster.client.java.sql.SqlDataType.*;

/**
 * Utility class for casting values to various SQL data types. Provides methods
 * to safely cast objects into types such as boolean, integer, byte, short, long,
 * double, string, and DateTime. Throws an exception if the cast is not possible
 * or if the input value is null.
 */
public class SqlCastHelper {

    public static boolean castToBoolean(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof Integer) {
            return ((Integer) value) != 0;
        } else if (value instanceof String) {
            String strValue = (String) value;
            if (BooleanLiteralsHelper.isTrueLiteral(strValue)) {
                return BooleanLiteralsHelper.isTrueLiteral(strValue);
            } else {
                throw new IllegalArgumentException("Cannot cast to boolean: " + value);
            }
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            throw new IllegalArgumentException("Cannot cast to boolean: " + value);
        }
    }

    public static int castToInteger(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot cast to integer: " + value, e);
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            throw new IllegalArgumentException("Cannot cast to integer: " + value);
        }
    }

    public static byte castToByte(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof String) {
            try {
                return Byte.parseByte((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot cast to byte: " + value, e);
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? (byte) 1 : (byte) 0;
        } else if (value instanceof Number) {
            return ((Number)value).byteValue();
        } else {
            throw new IllegalArgumentException("Cannot cast to byte: " + value);
        }
    }

    public static short castToShort(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof String) {
            try {
                return Short.parseShort((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot cast to short: " + value, e);
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? (short) 1 : (short) 0;
        } else if (value instanceof Number) {
            return ((Number)value).shortValue();
        } else {
            throw new IllegalArgumentException("Cannot cast to short: " + value);
        }
    }

    public static long castToLong(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot cast to long: " + value, e);
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1L : 0L;
        } else if (value instanceof Number) {
            return ((Number)value).longValue();
        } else {
            throw new IllegalArgumentException("Cannot cast to long: " + value);
        }
    }

    public static double castToDouble(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot cast to double: " + value, e);
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        } else if (value instanceof Number) {
            return ((Number)value).doubleValue();
        } else {
            throw new IllegalArgumentException("Cannot cast to double: " + value);
        }
    }

    public static String castToString(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof String) {
            return (String) value;
        } else {
            return String.valueOf(value);
        }
    }

    public static Object castToDateTime(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof java.util.Date) {
            return value;
        } else if (value instanceof String) {
            return java.sql.Timestamp.valueOf((String) value);
        } else {
            throw new IllegalArgumentException("Cannot cast to DateTime: " + value);
        }
    }

    public static Object castTo(String dataTypeName, Object value) {
        Objects.requireNonNull(dataTypeName);
        SqlDataType dataType = fromString(dataTypeName);
        return castTo(dataType, value);
    }


    public static Object castTo(SqlDataType dataType, Object value) {
        Objects.requireNonNull(dataType);

        if (isBooleanType(dataType)) {
            return castToBoolean(value);
        }

        if (isIntegerType(dataType)) {
            return castToInteger(value);
        }

        if (isByteType(dataType)) {
            return castToByte(value);
        }

        if (isShortType(dataType)) {
            return castToShort(value);
        }

        if (isLongType(dataType)) {
            return castToLong(value);
        }

        if (isDoubleType(dataType)) {
            return castToDouble(value);
        }

        if (isStringType(dataType)) {
            return castToString(value);
        }

        if (SqlDataType.isDateTimeType(dataType)) {
            return castToDateTime(value);
        }

        throw new IllegalArgumentException("Must implement casting to " + dataType + ": " + value);
    }
}
