package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.sql.heuristic.ConversionHelper;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnTypeParserTest {

    @Test
    public void testValidInstantFormat() {
        String validInstant = "2025-01-22T14:30:00Z"; // ISO 8601 format
        Instant expected = Instant.parse(validInstant);
        Instant result = ColumnTypeParser.getAsInstant(validInstant);
        assertEquals(expected, result, "Should parse the ISO 8601 formatted string correctly.");
    }

    @Test
    public void testValidZonedDateTimeFormat() {
        String zonedDateTime = "2025-01-22T14:30:00+00:00"; // ZonedDateTime format
        Instant expected = ZonedDateTime.parse(zonedDateTime).toInstant();
        Instant result = ColumnTypeParser.getAsInstant(zonedDateTime);
        assertEquals(expected, result, "Should parse the ZonedDateTime string correctly.");
    }

    @Test
    public void testValidOffsetDateTimeFormat() {
        String offsetDateTime = "2025-01-22 15:30:45+05:30"; // OffsetDateTime format

        // Define the DateTimeFormatter to match the input string pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");

        // Parse the string into a ZonedDateTime instance
        Instant expected = OffsetDateTime.parse(offsetDateTime, formatter).toInstant();

        Instant result = ColumnTypeParser.getAsInstant(offsetDateTime);
        assertEquals(expected, result, "Should parse the OffsetDateTime string correctly.");
    }

    @Test
    public void testValidCustomDateFormat() {
        String customDate = "22-Feb-25"; // Custom date format
        Instant expected = LocalDate.parse(customDate, DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH))
                .atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant result = ColumnTypeParser.getAsInstant(customDate);
        assertEquals(expected, result, "Should parse the custom date format correctly.");
    }

    @Test
    public void testValidDateWithTime() throws ParseException {
        String dateWithTime = "2025-01-22 14:30:00.1234"; // Format yyyy-MM-dd HH:mm:ss.SSSS
        Instant expected = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS").parse(dateWithTime).toInstant();
        Instant result = ColumnTypeParser.getAsInstant(dateWithTime);
        assertEquals(expected, result, "Should parse the date with time format correctly.");
    }

    @Test
    public void testValidDateWithoutTime() throws ParseException {
        String dateWithoutTime = "2025-01-22"; // Format yyyy-MM-dd
        Instant expected = new SimpleDateFormat("yyyy-MM-dd").parse(dateWithoutTime).toInstant();
        Instant result = ColumnTypeParser.getAsInstant(dateWithoutTime);
        assertEquals(expected, result, "Should parse the date without time correctly.");
    }

    @Test
    public void testInvalidDateFormat() throws ParseException {
        String invalidDate = "2025-13-01"; // Invalid date format
        Instant result = ColumnTypeParser.getAsInstant(invalidDate);
        Instant expectedInstant = new SimpleDateFormat("yyyy-MM-dd").parse("2026-01-01").toInstant();
        assertEquals(expectedInstant, result);
    }

    @Test
    public void testNullInput() {
        assertThrows(NullPointerException.class, () -> ColumnTypeParser.getAsInstant(null));
    }

    @Test
    public void testInvalidDateTime() {
        String invalidDateTime = "invalid-date-time"; // Completely invalid input
        assertThrows(IllegalArgumentException.class, () -> ColumnTypeParser.getAsInstant(invalidDateTime));
    }

    @Test
    public void testSameOutcome() {
        String timeAsString = "12:30:45";
        Time time = Time.valueOf(timeAsString);

        Instant instantFromTime = ConversionHelper.convertToInstant(time);
        Instant instantFromString = ColumnTypeParser.getAsInstant(timeAsString);
        assertEquals(instantFromTime,instantFromString);
    }

    @Test
    public void testInvalidDateTimeWithEnclosingQuotes() {
        String dateWithTime = "'2025-01-22 15:30:45'";
        assertThrows(IllegalArgumentException.class , () -> ColumnTypeParser.getAsInstant(dateWithTime));
    }
}
