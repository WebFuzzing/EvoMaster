package org.evomaster.client.java.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IsoDurationParserTest {

    private static void assertDuration(IsoDuration actual, long years, long months, long weeks, long days,
                                        long hours, long minutes, long seconds) {
        assertEquals(years, actual.years);
        assertEquals(months, actual.months);
        assertEquals(weeks, actual.weeks);
        assertEquals(days, actual.days);
        assertEquals(hours, actual.hours);
        assertEquals(minutes, actual.minutes);
        assertEquals(seconds, actual.seconds);
    }

    // -------------------------------------------------------------------------
    // General format
    // -------------------------------------------------------------------------

    @Test
    void generalFormat_dateOnly_parsesYearsMonthsDays() {
        assertDuration(IsoDurationParser.parse("P1Y2M3D"), 1, 2, 0, 3, 0, 0, 0);
    }

    @Test
    void generalFormat_timeOnly_parsesHoursMinutesSeconds() {
        assertDuration(IsoDurationParser.parse("PT4H5M6S"), 0, 0, 0, 0, 4, 5, 6);
    }

    @Test
    void generalFormat_dateAndTimeCombined_parsesAllComponents() {
        assertDuration(IsoDurationParser.parse("P1Y2M3DT4H5M6S"), 1, 2, 0, 3, 4, 5, 6);
    }

    @Test
    void generalFormat_lowercasePrefix_isAccepted() {
        assertDuration(IsoDurationParser.parse("p1y2m3d"), 1, 2, 0, 3, 0, 0, 0);
    }

    @Test
    void generalFormat_barePrefix_parsesAsZeroDuration() {
        // Mirrors the driver's own grammar, where every component is optional: a bare "P" is a
        // valid, if degenerate, zero-length duration rather than an error.
        assertDuration(IsoDurationParser.parse("P"), 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void generalFormat_bareTimeDesignatorWithNoValue_parsesAsZeroDuration() {
        assertDuration(IsoDurationParser.parse("PT"), 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void generalFormat_dateOnlyWithBareTrailingTimeDesignator_parsesDateComponents() {
        assertDuration(IsoDurationParser.parse("P1Y2M3DT"), 1, 2, 0, 3, 0, 0, 0);
    }

    @Test
    void generalFormat_malformedContent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> IsoDurationParser.parse("P1X"));
    }

    // -------------------------------------------------------------------------
    // Weeks-only format
    // -------------------------------------------------------------------------

    @Test
    void weekFormat_parsesWeeks() {
        assertDuration(IsoDurationParser.parse("P3W"), 0, 0, 3, 0, 0, 0, 0);
    }

    @Test
    void weekFormat_lowercasePrefix_isAccepted() {
        assertDuration(IsoDurationParser.parse("p3w"), 0, 0, 3, 0, 0, 0, 0);
    }

    @Test
    void weekFormat_mixedWithOtherUnits_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> IsoDurationParser.parse("P1Y2W"));
    }

    // -------------------------------------------------------------------------
    // Alternative format
    // -------------------------------------------------------------------------

    @Test
    void alternativeFormat_parsesAllComponents() {
        assertDuration(IsoDurationParser.parse("P0001-02-03T04:05:06"), 1, 2, 0, 3, 4, 5, 6);
    }

    @Test
    void alternativeFormat_lowercasePrefix_isAccepted() {
        assertDuration(IsoDurationParser.parse("p0000-01-00T00:00:00"), 0, 1, 0, 0, 0, 0, 0);
    }

    @Test
    void alternativeFormat_missingTimePart_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> IsoDurationParser.parse("P2024-01"));
    }

    // -------------------------------------------------------------------------
    // Errors
    // -------------------------------------------------------------------------

    @Test
    void parse_notPrefixed_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> IsoDurationParser.parse("3d"));
    }
}