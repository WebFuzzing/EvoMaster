package org.evomaster.client.java.controller.cassandra.parser;

import org.evomaster.client.java.controller.cassandra.model.CqlDurationLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CqlDurationLiteralParserTest {

    private static final long NANOS_PER_HOUR = 3_600_000_000_000L;
    private static final long NANOS_PER_MINUTE = 60_000_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final long NANOS_PER_MICROSECOND = 1_000L;

    private static void assertDuration(CqlDurationLiteral actual, int months, int days, long nanos) {
        assertEquals(months, actual.months);
        assertEquals(days, actual.days);
        assertEquals(nanos, actual.nanos);
    }

    // -------------------------------------------------------------------------
    // Standard Cassandra format
    // -------------------------------------------------------------------------

    @Test
    void standardFormat_singleUnit_parsesDays() {
        assertDuration(CqlDurationLiteralParser.parse("3d"), 0, 3, 0);
    }

    @Test
    void standardFormat_multipleUnitsCombined_parsesAllComponents() {
        long expectedNanos = 4 * NANOS_PER_HOUR + 5 * NANOS_PER_MINUTE + 6 * NANOS_PER_SECOND;
        assertDuration(CqlDurationLiteralParser.parse("1y2mo3d4h5m6s"), 14, 3, expectedNanos);
    }

    @Test
    void standardFormat_subSecondUnits_parsesNanos() {
        long expectedNanos = 7 * NANOS_PER_MILLISECOND + 8 * NANOS_PER_MICROSECOND + 9;
        assertDuration(CqlDurationLiteralParser.parse("7ms8us9ns"), 0, 0, expectedNanos);
    }

    @Test
    void standardFormat_microsecondMuVariant_parsesNanos() {
        assertDuration(CqlDurationLiteralParser.parse("8µs"), 0, 0, 8 * NANOS_PER_MICROSECOND);
    }

    @Test
    void standardFormat_caseInsensitive_parsesUppercaseUnits() {
        assertDuration(CqlDurationLiteralParser.parse("1Y2MO3D"), 14, 3, 0);
    }

    @Test
    void standardFormat_repeatedUnit_accumulates() {
        assertDuration(CqlDurationLiteralParser.parse("1d2d"), 0, 3, 0);
    }

    @Test
    void standardFormat_weeks_convertToDays() {
        assertDuration(CqlDurationLiteralParser.parse("2w3d"), 0, 17, 0);
    }

    @Test
    void standardFormat_negative_negatesAllComponents() {
        assertDuration(CqlDurationLiteralParser.parse("-1mo3d"), -1, -3, 0);
    }

    @Test
    void standardFormat_negativeWeeks_negatesDays() {
        assertDuration(CqlDurationLiteralParser.parse("-2w"), 0, -14, 0);
    }

    @Test
    void standardFormat_isolatedHour_parsesNanos() {
        assertDuration(CqlDurationLiteralParser.parse("5h"), 0, 0, 5 * NANOS_PER_HOUR);
    }

    @Test
    void standardFormat_isolatedMinute_parsesNanos() {
        assertDuration(CqlDurationLiteralParser.parse("5m"), 0, 0, 5 * NANOS_PER_MINUTE);
    }

    @Test
    void standardFormat_isolatedSecond_parsesNanos() {
        assertDuration(CqlDurationLiteralParser.parse("5s"), 0, 0, 5 * NANOS_PER_SECOND);
    }

    @Test
    void standardFormat_unrecognizedCharacters_areSilentlyIgnored() {
        // Documents current lenient behavior: the standard-format branch has no "whole string
        // consumed" check, so unrecognized characters (here, "5x") are silently skipped rather
        // than rejected.
        assertDuration(CqlDurationLiteralParser.parse("5x3d"), 0, 3, 0);
    }

    // -------------------------------------------------------------------------
    // ISO 8601 format
    // -------------------------------------------------------------------------

    @Test
    void isoFormat_dateOnly_parsesMonthsAndDays() {
        assertDuration(CqlDurationLiteralParser.parse("P1Y2M3D"), 14, 3, 0);
    }

    @Test
    void isoFormat_timeOnly_parsesNanos() {
        long expectedNanos = 4 * NANOS_PER_HOUR + 5 * NANOS_PER_MINUTE + 6 * NANOS_PER_SECOND;
        assertDuration(CqlDurationLiteralParser.parse("PT4H5M6S"), 0, 0, expectedNanos);
    }

    @Test
    void isoFormat_dateAndTimeCombined_parsesAllComponents() {
        long expectedNanos = 4 * NANOS_PER_HOUR + 5 * NANOS_PER_MINUTE + 6 * NANOS_PER_SECOND;
        assertDuration(CqlDurationLiteralParser.parse("P1Y2M3DT4H5M6S"), 14, 3, expectedNanos);
    }

    @Test
    void isoFormat_lowercasePrefix_isAccepted() {
        assertDuration(CqlDurationLiteralParser.parse("p1y2m3d"), 14, 3, 0);
    }

    @Test
    void isoFormat_negativeDateOnly_negatesMonthsAndDays() {
        assertDuration(CqlDurationLiteralParser.parse("-P1Y2M3D"), -14, -3, 0);
    }

    @Test
    void isoFormat_negativeTimeOnly_negatesNanos() {
        assertDuration(CqlDurationLiteralParser.parse("-PT5H"), 0, 0, -5 * NANOS_PER_HOUR);
    }

    @Test
    void isoFormat_negativeDateAndTimeCombined_negatesAllComponents() {
        long expectedNanos = 4 * NANOS_PER_HOUR + 5 * NANOS_PER_MINUTE + 6 * NANOS_PER_SECOND;
        assertDuration(CqlDurationLiteralParser.parse("-P1Y2M3DT4H5M6S"), -14, -3, -expectedNanos);
    }

    @Test
    void isoFormat_barePrefix_parsesAsZeroDuration() {
        // Mirrors the driver's own grammar, where every component is optional: a bare "P" is a
        // valid, if degenerate, zero-length duration rather than an error.
        assertDuration(CqlDurationLiteralParser.parse("P"), 0, 0, 0);
    }

    @Test
    void isoFormat_bareTimeDesignatorWithNoValue_parsesAsZeroDuration() {
        assertDuration(CqlDurationLiteralParser.parse("PT"), 0, 0, 0);
    }

    @Test
    void isoFormat_dateOnlyWithBareTrailingTimeDesignator_parsesDateComponents() {
        assertDuration(CqlDurationLiteralParser.parse("P1Y2M3DT"), 14, 3, 0);
    }

    @Test
    void isoFormat_malformedContent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse("P1X"));
    }

    // -------------------------------------------------------------------------
    // ISO 8601, weeks-only format
    // -------------------------------------------------------------------------

    @Test
    void isoWeekFormat_parsesDaysFromWeeks() {
        assertDuration(CqlDurationLiteralParser.parse("P3W"), 0, 21, 0);
    }

    @Test
    void isoWeekFormat_negative_negatesDays() {
        assertDuration(CqlDurationLiteralParser.parse("-P3W"), 0, -21, 0);
    }

    @Test
    void isoWeekFormat_lowercasePrefix_isAccepted() {
        assertDuration(CqlDurationLiteralParser.parse("p3w"), 0, 21, 0);
    }

    @Test
    void isoWeekFormat_mixedWithOtherUnits_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse("P1Y2W"));
    }

    // -------------------------------------------------------------------------
    // ISO 8601, alternative format
    // -------------------------------------------------------------------------

    @Test
    void isoAlternativeFormat_parsesAllComponents() {
        long expectedNanos = 4 * NANOS_PER_HOUR + 5 * NANOS_PER_MINUTE + 6 * NANOS_PER_SECOND;
        assertDuration(CqlDurationLiteralParser.parse("P0001-02-03T04:05:06"), 14, 3, expectedNanos);
    }

    @Test
    void isoAlternativeFormat_negative_negatesAllComponents() {
        long expectedNanos = 4 * NANOS_PER_HOUR + 5 * NANOS_PER_MINUTE + 6 * NANOS_PER_SECOND;
        assertDuration(CqlDurationLiteralParser.parse("-P0001-02-03T04:05:06"), -14, -3, -expectedNanos);
    }

    @Test
    void isoAlternativeFormat_lowercasePrefix_isAccepted() {
        assertDuration(CqlDurationLiteralParser.parse("p0000-01-00T00:00:00"), 1, 0, 0);
    }

    @Test
    void isoAlternativeFormat_missingTimePart_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse("P2024-01"));
    }

    // -------------------------------------------------------------------------
    // Errors
    // -------------------------------------------------------------------------

    @Test
    void parse_null_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse(null));
    }

    @Test
    void parse_empty_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse(""));
    }

    @Test
    void parse_blank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse("   "));
    }

    @Test
    void parse_bareMinusSign_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CqlDurationLiteralParser.parse("-"));
    }
}