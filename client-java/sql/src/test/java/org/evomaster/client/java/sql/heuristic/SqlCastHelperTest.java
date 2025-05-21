package org.evomaster.client.java.sql.heuristic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlCastHelperTest {

    @Test
    public void testCastToBooleanWithInteger() {
        assertTrue(SqlCastHelper.castToBoolean(1));
        assertFalse(SqlCastHelper.castToBoolean(0));
    }

    @Test
    public void testCastToBooleanWithString() {
        assertTrue(SqlCastHelper.castToBoolean("true"));
        assertTrue(SqlCastHelper.castToBoolean("yes"));
        assertTrue(SqlCastHelper.castToBoolean("on"));
        assertTrue(SqlCastHelper.castToBoolean("t"));
    }

    @Test
    public void testCastToBooleanWithBoolean() {
        assertTrue(SqlCastHelper.castToBoolean(true));
        assertFalse(SqlCastHelper.castToBoolean(false));
    }

    @Test
    public void testCastToBooleanWithNull() {
        assertThrows(NullPointerException.class, () -> {
            SqlCastHelper.castToBoolean(null);
        });
    }

    @Test
    public void testCastToBooleanWithInvalidString() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean("invalid");
        });
    }

    @Test
    public void testCastToBooleanWithEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean("");
        });
    }

    @Test
    public void testCastToBooleanWithNonIntegerNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean(2.5);
        });
    }

    @Test
    public void testCastToBooleanWithNonStringObject() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean(new Object());
        });
    }

    @Test
    public void testCastToBooleanWithUppercaseTrueAndYes() {
        assertTrue(SqlCastHelper.castToBoolean("TRUE"));
        assertTrue(SqlCastHelper.castToBoolean("YES"));
    }

    @Test
    public void testCastToBooleanWithNegativeInteger() {
        assertTrue(SqlCastHelper.castToBoolean(-1));
    }

    @Test
    public void testCastToBooleanWithInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean(1.5);
        });
    }

    @Test
    public void testCastToBooleanWithFalseLiteral() {
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean("false");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean("no");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean("off");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SqlCastHelper.castToBoolean("f");
        });
    }

    @Test
    public void testCastToBoolean() {
        assertTrue((Boolean) SqlCastHelper.castTo("BOOLEAN", 1));
        assertFalse((Boolean) SqlCastHelper.castTo("BOOLEAN", 0));
        assertTrue((Boolean) SqlCastHelper.castTo("BOOLEAN", "true"));
    }

    @Test
    public void testCastToInteger() {
        assertEquals(42, SqlCastHelper.castTo("INTEGER", 42));
        assertEquals(42, SqlCastHelper.castTo("INTEGER", "42"));
        assertEquals(0, SqlCastHelper.castTo("INTEGER", false));
    }

    @Test
    public void testCastToByte() {
        assertEquals((byte) 10, SqlCastHelper.castTo("TINYINT", (byte) 10));
        assertEquals((byte) 10, SqlCastHelper.castTo("TINYINT", "10"));
        assertEquals((byte) 1, SqlCastHelper.castTo("TINYINT", true));
    }

    @Test
    public void testCastToShort() {
        assertEquals((short) 123, SqlCastHelper.castTo("SMALLINT", (short) 123));
        assertEquals((short) 123, SqlCastHelper.castTo("SMALLINT", "123"));
        assertEquals((short) 0, SqlCastHelper.castTo("SMALLINT", false));
    }

    @Test
    public void testCastToLong() {
        assertEquals(9876543210L, SqlCastHelper.castTo("BIGINT", 9876543210L));
        assertEquals(9876543210L, SqlCastHelper.castTo("BIGINT", "9876543210"));
        assertEquals(0L, SqlCastHelper.castTo("BIGINT", false));
    }

    @Test
    public void testCastToDouble() {
        assertEquals(123.45, SqlCastHelper.castTo("DOUBLE", 123.45));
        assertEquals(123.45, SqlCastHelper.castTo("DOUBLE", "123.45"));
        assertEquals(1.0, SqlCastHelper.castTo("DOUBLE", true));
    }

    @Test
    public void testCastToString() {
        assertEquals("hello", SqlCastHelper.castTo("VARCHAR", "hello"));
        assertEquals("42", SqlCastHelper.castTo("VARCHAR", 42));
        assertEquals("true", SqlCastHelper.castTo("VARCHAR", true));
    }

    @Test
    public void testCastToDateTime() {
        assertEquals(java.sql.Timestamp.valueOf("2023-01-01 12:00:00"),
                SqlCastHelper.castTo("DATETIME", "2023-01-01 12:00:00"));
        assertEquals(new java.util.Date(0), SqlCastHelper.castTo("DATETIME", new java.util.Date(0)));
    }

    @Test
    public void testCastToInvalid() {
        assertThrows(IllegalArgumentException.class, () -> SqlCastHelper.castTo("BOOLEAN", "not_a_boolean"));
        assertThrows(IllegalArgumentException.class, () -> SqlCastHelper.castTo("INTEGER", "not_an_integer"));
        assertThrows(IllegalArgumentException.class, () -> SqlCastHelper.castTo("DATETIME", 12345));
    }

    @Test
    public void castToInteger() {
        Integer actual = SqlCastHelper.castToInteger(1.0d);
        assertEquals(1, actual.intValue());
    }
}
