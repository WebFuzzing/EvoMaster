package org.evomaster.client.java.sql.heuristic.function;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class DateTruncTest {

    @Test
    void localDateTimeTruncateToMinute() {
        LocalDateTime dt = LocalDateTime.of(2020, 5, 10, 12, 34, 56, 789_000_000);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("minute", dt);
        assertTrue(res instanceof LocalDateTime);
        assertEquals(LocalDateTime.of(2020, 5, 10, 12, 34, 0, 0), res);
    }

    @Test
    void localTimeTruncateToHour() {
        LocalTime t = LocalTime.of(3, 45, 12, 123_000_000);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("hour", t);
        assertTrue(res instanceof LocalTime);
        assertEquals(LocalTime.of(3, 0, 0), res);
    }

    @Test
    void instantTruncateToDay() {
        Instant i = Instant.parse("2020-05-10T12:34:56.789Z");
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("day", i);
        assertTrue(res instanceof Instant);
        assertEquals(Instant.parse("2020-05-10T00:00:00Z"), res);
    }

    @Test
    void offsetDateTimeTruncateToMonthKeepsOffset() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 3, 15, 10, 11, 12, 345_000_000, ZoneOffset.ofHours(-5));
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("month", odt);
        assertTrue(res instanceof OffsetDateTime);
        assertEquals(OffsetDateTime.of(2021, 3, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), res);
    }

    @Test
    void zonedDateTimeTruncateToYearPreservesZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2019, 7, 20, 15, 30, 45, 999_000_000, ZoneId.of("Europe/Rome"));
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("year", zdt);
        assertTrue(res instanceof ZonedDateTime);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneId.of("Europe/Rome"));
        assertEquals(expected, res);
    }

    @Test
    void localDateTruncateToMonth() {
        LocalDate d = LocalDate.of(2019, 12, 25);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("month", d);
        assertTrue(res instanceof LocalDate);
        assertEquals(LocalDate.of(2019, 12, 1), res);
    }

    @Test
    void offsetTimeTruncateToSecond() {
        OffsetTime t = OffsetTime.of(12, 30, 45, 987_000_000, ZoneOffset.ofHours(2));
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("second", t);
        assertTrue(res instanceof OffsetTime);
        assertEquals(OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2)), res);
    }

    @Test
    void unsupportedUnitForLocalTimeThrows() {
        LocalTime t = LocalTime.of(1, 2, 3);
        DateTrunc f = new DateTrunc();
        assertThrows(IllegalArgumentException.class, () -> f.evaluate("month", t));
    }

    @Test
    void nullTimestampReturnsNull() {
        DateTrunc f = new DateTrunc();
        assertNull(f.evaluate("day", (Object) null));
    }

    @Test
    void unsupportedTemporalAccessorTypeThrows() {
        // Year implements TemporalAccessor but is not handled by DateTrunc
        Year y = Year.of(2020);
        DateTrunc f = new DateTrunc();
        assertThrows(UnsupportedOperationException.class, () -> f.evaluate("day", y));
    }

    @Test
    void localDateTruncateToDayReturnsSameDate() {
        LocalDate d = LocalDate.of(2021, 8, 17);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("day", d);
        assertTrue(res instanceof LocalDate);
        assertEquals(LocalDate.of(2021, 8, 17), res);
    }

    @Test
    void localDateTruncateToYear() {
        LocalDate d = LocalDate.of(2018, 6, 30);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("year", d);
        assertTrue(res instanceof LocalDate);
        assertEquals(LocalDate.of(2018, 1, 1), res);
    }

    @Test
    void localDateUnsupportedUnitThrows() {
        LocalDate d = LocalDate.of(2020, 1, 1);
        DateTrunc f = new DateTrunc();
        assertThrows(IllegalArgumentException.class, () -> f.evaluate("second", d));
    }

    @Test
    void localDateTimeTruncateToSecond() {
        LocalDateTime dt = LocalDateTime.of(2022, 1, 2, 3, 4, 5, 123_456_789);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("second", dt);
        assertTrue(res instanceof LocalDateTime);
        assertEquals(LocalDateTime.of(2022, 1, 2, 3, 4, 5, 0), res);
    }

    @Test
    void localDateTimeTruncateToMonth() {
        LocalDateTime dt = LocalDateTime.of(2022, 7, 15, 10, 11, 12, 345_000_000);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("month", dt);
        assertTrue(res instanceof LocalDateTime);
        assertEquals(LocalDateTime.of(2022, 7, 1, 0, 0), res);
    }

    @Test
    void localDateTimeTruncateToYear() {
        LocalDateTime dt = LocalDateTime.of(1999, 12, 31, 23, 59, 59, 999_000_000);
        DateTrunc f = new DateTrunc();
        Object res = f.evaluate("year", dt);
        assertTrue(res instanceof LocalDateTime);
        assertEquals(LocalDateTime.of(1999, 1, 1, 0, 0), res);
    }

    @Test
    void localDateTimeUnsupportedUnitThrows() {
        LocalDateTime dt = LocalDateTime.of(2020, 2, 29, 12, 0);
        DateTrunc f = new DateTrunc();
        assertThrows(IllegalArgumentException.class, () -> f.evaluate("unsupported_unit", dt));
    }
}
