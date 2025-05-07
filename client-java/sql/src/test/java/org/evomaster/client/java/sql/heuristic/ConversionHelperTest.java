package org.evomaster.client.java.sql.heuristic;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ConversionHelperTest {

    public static final LocalDate UNIX_EPOCH_LOCAL_DATE = LocalDate.of(1970, 1, 1);

    @Test
    public void testNull() {
        Object input = null;

        Instant result = ConversionHelper.convertToInstant(input);

        assertNull(result, "Result should be null when input is null");
    }

    @Test
    public void testSQLTime() {
        LocalTime localTime = LocalTime.of(10, 30, 45, 123456789); // 10:30:45.123456789
        Time sqlTime = Time.valueOf(localTime);

        LocalDateTime localDateTime = LocalDateTime.of(UNIX_EPOCH_LOCAL_DATE, sqlTime.toLocalTime());
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        Instant expected = zonedDateTime.toInstant();

        Instant result = ConversionHelper.convertToInstant(sqlTime);

        assertEquals(0, sqlTime.toLocalTime().getNano()); // valueOf truncates nanoseconds
        assertEquals(expected, result, "Result should match the Instant derived from the SQL Time input");
    }

    @Test
    public void testDate() {
        Date date = new Date();
        Instant expected = date.toInstant();

        Instant result = ConversionHelper.convertToInstant(date);

        assertEquals(expected, result, "Result should match the Instant derived from the java.util.Date input");
    }

    @Test
    public void testOffsetDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        Instant expected = offsetDateTime.toInstant();

        Instant result = ConversionHelper.convertToInstant(offsetDateTime);

        assertEquals(expected, result, "Result should match the Instant derived from the OffsetDateTime input");
    }

    @Test
    public void testString() {
        String dateTimeString = "2023-01-01T10:30:45Z";
        Instant expected = Instant.parse(dateTimeString);

        Instant result = ConversionHelper.convertToInstant(dateTimeString);

        assertEquals(expected, result, "Result should match the Instant parsed from the String input");
    }

    @Test
    public void testInvalidInput() {
        Object invalidInput = 12345;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ConversionHelper.convertToInstant(invalidInput));

        assertTrue(exception.getMessage().contains("Argument must be date, local date time or string"),
                "Exception message should mention the invalid argument type");
    }

    @Test
    public void testSameOutcome() {
        String timeAsString = "12:30:45";
        Time time = Time.valueOf(timeAsString);

        Instant instantFromTime = ConversionHelper.convertToInstant(time);
        Instant instantFromString = ConversionHelper.convertToInstant(timeAsString);
        assertEquals(instantFromTime, instantFromString);
    }

    @Test
    public void convertPositiveYearToInstant() throws ParseException {
        Long year = 2023L;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Instant expected = sdf.parse("2023-01-01").toInstant();
        Instant actual = ConversionHelper.convertToInstant(year);
        assertEquals(expected, actual);
    }

    @Test
    public void convertNegativeYearLongToInstant() throws ParseException {
        Long invalidYear = -1L;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Instant expected = sdf.parse("-1-01-01").toInstant();
        Instant actual = ConversionHelper.convertToInstant(invalidYear);
        assertEquals(expected, actual);
    }
}
