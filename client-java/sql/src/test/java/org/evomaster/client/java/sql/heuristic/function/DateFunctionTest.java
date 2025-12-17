package org.evomaster.client.java.sql.heuristic.function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateFunctionTest {

    @Test
    void testEvaluateWithNullArgument() {
        Object result = new DateFunction().evaluate((Object) null);
        assertNull(result);
    }

    @Test
    void testEvaluateWithNoArguments() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DateFunction().evaluate()
        );
        assertTrue(exception.getMessage().contains("exactly one argument"));
    }

    @Test
    void testEvaluateWithMultipleArguments() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DateFunction().evaluate("2023-12-25", "2023-12-26")
        );
        assertTrue(exception.getMessage().contains("exactly one argument"));
    }

    @Test
    void testEvaluateWithSqlDate() {
        Date inputDate = Date.valueOf("2023-12-25");
        Object result = new DateFunction().evaluate(inputDate);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(inputDate, result);
    }

    @Test
    void testEvaluateWithSqlTimestamp() {
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 14:30:45.123");
        Object result = new DateFunction().evaluate(timestamp);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithSqlTimestampAtMidnight() {
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 00:00:00.000");
        Object result = new DateFunction().evaluate(timestamp);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithSqlTimestampEndOfDay() {
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 23:59:59.999");
        Object result = new DateFunction().evaluate(timestamp);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithJavaUtilDate() {
        LocalDateTime localDateTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
        java.util.Date utilDate = java.sql.Timestamp.valueOf(localDateTime);

        Object result = new DateFunction().evaluate(utilDate);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithSqlTime() {
        // java.sql.Time extends java.util.Date
        Time time = Time.valueOf("14:30:45");

        Object result = new DateFunction().evaluate(time);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        // The date will be derived from the epoch date or system default
    }

    @Test
    void testEvaluateWithStringIsoFormat() {
        String dateString = "2023-12-25";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithStringDateTimeFormat() {
        String dateString = "2023-12-25 14:30:45";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithStringIsoDateTimeFormat() {
        String dateString = "2023-12-25T14:30:45";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithStringWithMilliseconds() {
        String dateString = "2023-12-25 14:30:45.123";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithStringWithMicroseconds() {
        String dateString = "2023-12-25 14:30:45.123456";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithStringWithWhitespace() {
        String dateString = "  2023-12-25  ";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateWithEmptyString() {
        String dateString = "";
        Object result = new DateFunction().evaluate(dateString);
        assertNull(result);
    }

    @Test
    void testEvaluateWithWhitespaceOnlyString() {
        String dateString = "   ";
        Object result = new DateFunction().evaluate(dateString);
        assertNull(result);
    }

    @Test
    void testEvaluateWithInvalidStringFormat() {
        String dateString = "25-12-2023";
        assertThrows(IllegalArgumentException.class, () -> new DateFunction().evaluate(dateString));
    }

    @Test
    void testEvaluateWithInvalidDate() {
        String dateString = "2023-02-30";
        assertThrows(IllegalArgumentException.class, () -> new DateFunction().evaluate(dateString));
    }

    @Test
    void testEvaluateWithUnsupportedType() {
        Integer unsupportedArg = 12345;
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DateFunction().evaluate(unsupportedArg)
        );
        assertTrue(exception.getMessage().contains("Unsupported type for DATE()"));
    }

    @Test
    void testEvaluateWithLeapYearDate() {
        String dateString = "2024-02-29";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2024-02-29"), result);
    }

    @Test
    void testEvaluateWithDateStartOfYear() {
        String dateString = "2023-01-01";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-01-01"), result);
    }

    @Test
    void testEvaluateWithDateEndOfYear() {
        String dateString = "2023-12-31";
        Object result = new DateFunction().evaluate(dateString);

        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-31"), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2023-01-01",
            "2023-06-15",
            "2023-12-31",
            "2024-02-29",
            "2000-01-01",
            "1999-12-31"
    })
    void testEvaluateWithVariousValidDates(String dateString) {
        Object result = new DateFunction().evaluate(dateString);
        assertNotNull(result);
        assertTrue(result instanceof Date);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2023-12-25 00:00:00",
            "2023-12-25 12:00:00",
            "2023-12-25 23:59:59",
            "2023-12-25T14:30:45",
            "2023-12-25 14:30:45.123",
            "2023-12-25 14:30:45.123456"
    })
    void testEvaluateWithVariousDateTimeFormats(String dateString) {
        Object result = new DateFunction().evaluate(dateString);
        assertNotNull(result);
        assertTrue(result instanceof Date);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testEvaluateExtractsOnlyDateFromTimestamp() {
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 14:30:45.123456789");
        Object result = new DateFunction().evaluate(timestamp);

        assertNotNull(result);
        Date date = (Date) result;
        LocalDate localDate = date.toLocalDate();

        assertEquals(2023, localDate.getYear());
        assertEquals(12, localDate.getMonthValue());
        assertEquals(25, localDate.getDayOfMonth());
    }

    @Test
    void testEvaluateConsistencyAcrossTypes() {
        Date sqlDate = Date.valueOf("2023-12-25");
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 14:30:45");
        String isoString = "2023-12-25";
        String dateTimeString = "2023-12-25 14:30:45";

        Object result1 = new DateFunction().evaluate(sqlDate);
        Object result2 = new DateFunction().evaluate(timestamp);
        Object result3 = new DateFunction().evaluate(isoString);
        Object result4 = new DateFunction().evaluate(dateTimeString);

        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals(result3, result4);
    }

    @Test
    void testEvaluateWithBoundaryDates() {
        // Test minimum date
        Date minDate = Date.valueOf("0001-01-01");
        Object result1 = new DateFunction().evaluate(minDate);
        assertEquals(minDate, result1);

        // Test far future date
        Date maxDate = Date.valueOf("9999-12-31");
        Object result2 = new DateFunction().evaluate(maxDate);
        assertEquals(maxDate, result2);
    }

    @Test
    void testEvaluateWithDoubleType() {
        Double doubleArg = 123.45;
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DateFunction().evaluate(doubleArg)
        );
        assertTrue(exception.getMessage().contains("Unsupported type"));
    }

    @Test
    void testEvaluateWithBooleanType() {
        Boolean booleanArg = true;
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DateFunction().evaluate(booleanArg)
        );
        assertTrue(exception.getMessage().contains("Unsupported type"));
    }

}
