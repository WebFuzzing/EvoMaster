package org.evomaster.client.java.sql.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SqlDateTimeParserTest {

    @Test
    void testParseIsoLocalDate() {
        String dateString = "2023-12-25";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseDateWithLeadingZeros() {
        String dateString = "2023-01-05";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-01-05"), result);
    }

    @Test
    void testParseDateLeapYear() {
        String dateString = "2024-02-29";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2024-02-29"), result);
    }

    @Test
    void testParseDateEndOfYear() {
        String dateString = "2023-12-31";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-31"), result);
    }

    @Test
    void testParseDateStartOfYear() {
        String dateString = "2023-01-01";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-01-01"), result);
    }

    @Test
    void testParseIsoLocalDateTime() {
        // Format: yyyy-MM-ddTHH:mm:ss
        String dateString = "2023-12-25T14:30:45";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseMySqlClassicFormat() {
        // Format: yyyy-MM-dd HH:mm:ss
        String dateString = "2023-12-25 14:30:45";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseDateTimeWithMilliseconds() {
        // Format: yyyy-MM-dd HH:mm:ss.SSS
        String dateString = "2023-12-25 14:30:45.123";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseDateTimeWithMicroseconds() {
        // Format: yyyy-MM-dd HH:mm:ss.SSSSSS
        String dateString = "2023-12-25 14:30:45.123456";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseDateTimeWithMinutes() {
        // Format: yyyy-MM-dd HH:mm
        String dateString = "2023-12-25 14:30";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseDateTimeAtMidnight() {
        String dateString = "2023-12-25 00:00:00";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseDateTimeEndOfDay() {
        String dateString = "2023-12-25 23:59:59";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @Test
    void testParseNullDate() {
        assertThrows(NullPointerException.class, () -> new SqlDateTimeParser().parseDate(null));
    }

    @Test
    void testParseEmptyDate() {
        String dateString = "";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidDateFormat() {
        String dateString = "25-12-2023";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidDate() {
        String dateString = "2023-02-30";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidMonth() {
        String dateString = "2023-13-01";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidDay() {
        String dateString = "2023-12-32";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidLeapYear() {
        String dateString = "2023-02-29";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidTime() {
        String dateString = "2023-12-25 25:00:00";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidMinutes() {
        String dateString = "2023-12-25 14:65:00";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseInvalidSeconds() {
        String dateString = "2023-12-25 14:30:70";
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseUnsupportedFormat() {
        String dateString = "12/25/2023";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SqlDateTimeParser().parseDate(dateString)
        );
        assertTrue(exception.getMessage().contains("unsupported date format"));
    }

    @Test
    void testParseDateBoundaryMinimum() {
        String dateString = "0001-01-01";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("0001-01-01"), result);
    }

    @Test
    void testParseDateBoundaryMaximum() {
        String dateString = "9999-12-31";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        assertEquals(Date.valueOf("9999-12-31"), result);
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
    void testParseVariousValidDates(String dateString) {
        Date result = new SqlDateTimeParser().parseDate(dateString);
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2023-12-25 00:00:00",
        "2023-12-25 12:00:00",
        "2023-12-25 23:59:59",
        "2023-12-25T14:30:45",
        "2023-12-25 14:30:45.123",
        "2023-12-25 14:30:45.123456",
        "2023-12-25 14:30"
    })
    void testParseVariousValidDateTimeFormats(String dateString) {
        Date result = new SqlDateTimeParser().parseDate(dateString);
        assertNotNull(result);
        assertEquals(Date.valueOf("2023-12-25"), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid",
        "2023/12/25",
        "25-12-2023",
        "2023-13-01",
        "2023-12-32",
        "2023-02-30",
        "2023-12-25 25:00:00",
        "not a date"
    })
    void testParseVariousInvalidFormats(String dateString) {
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseDateWithWhitespace() {
        String dateString = "  2023-12-25  ";
        // Trimming is not done by the parser, so this should fail
        assertThrows(IllegalArgumentException.class, () -> new SqlDateTimeParser().parseDate(dateString));
    }

    @Test
    void testParseDateExtractsOnlyDate() {
        String dateString = "2023-12-25 14:30:45";
        Date result = new SqlDateTimeParser().parseDate(dateString);

        assertNotNull(result);
        // Verify that only the date part is extracted, not time
        assertEquals(Date.valueOf("2023-12-25"), result);

        // Verify time component is not included
        LocalDate localDate = result.toLocalDate();
        assertEquals(2023, localDate.getYear());
        assertEquals(12, localDate.getMonthValue());
        assertEquals(25, localDate.getDayOfMonth());
    }

    @Test
    void testParseMultipleFormatsForSameDate() {
        String isoFormat = "2023-12-25";
        String dateTimeFormat = "2023-12-25 12:00:00";
        String isoDateTimeFormat = "2023-12-25T12:00:00";

        Date result1 = new SqlDateTimeParser().parseDate(isoFormat);
        Date result2 = new SqlDateTimeParser().parseDate(dateTimeFormat);
        Date result3 = new SqlDateTimeParser().parseDate(isoDateTimeFormat);

        // All should result in the same date
        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals(result1, result3);
    }

}
