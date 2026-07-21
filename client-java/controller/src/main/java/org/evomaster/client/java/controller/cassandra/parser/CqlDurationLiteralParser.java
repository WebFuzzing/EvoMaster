package org.evomaster.client.java.controller.cassandra.parser;

import org.evomaster.client.java.controller.cassandra.model.CqlDurationLiteral;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Cassandra duration string literal into a {@link CqlDurationLiteral}.
 *
 * <p>Supports four formats, all optionally prefixed with {@code -} for a negative duration,
 * mirroring {@code com.datastax.oss.driver.api.core.data.CqlDuration#from(String)}:
 *
 * <ul>
 *   <li>Standard Cassandra: {@code 1y2mo3w4d5h6m7s} (units, including weeks, may appear in any
 *       order)
 *   <li>ISO 8601: {@code P1Y2M3DT4H5M6S}
 *   <li>ISO 8601, weeks only: {@code P3W}
 *   <li>ISO 8601, alternative format: {@code P0001-02-03T04:05:06}
 * </ul>
 */
public class CqlDurationLiteralParser {

    private CqlDurationLiteralParser() {}

    private static final String UNIT_MONTH = "mo";
    private static final String UNIT_MILLISECOND = "ms";
    private static final String UNIT_MICROSECOND_MU = "µs";
    private static final String UNIT_MICROSECOND = "us";
    private static final String UNIT_NANOSECOND = "ns";
    private static final String UNIT_YEAR = "y";
    private static final String UNIT_WEEK = "w";
    private static final String UNIT_DAY = "d";
    private static final String UNIT_HOUR = "h";
    private static final String UNIT_MINUTE = "m";
    private static final String UNIT_SECOND = "s";

    private static final int  MONTHS_PER_YEAR = 12;
    private static final int  DAYS_PER_WEEK = 7;
    private static final long NANOS_PER_HOUR = 3_600_000_000_000L;
    private static final long NANOS_PER_MINUTE = 60_000_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final long NANOS_PER_MICROSECOND = 1_000L;

    private static final char ISO_TIME_SEPARATOR = 'T';
    private static final char ISO_YEAR_DESIGNATOR = 'Y';
    private static final char ISO_MONTH_OR_MINUTE_DESIGNATOR = 'M';
    private static final char ISO_DAY_DESIGNATOR = 'D';
    private static final char ISO_HOUR_DESIGNATOR = 'H';
    private static final char ISO_SECOND_DESIGNATOR = 'S';

    private static final String ISO_DURATION_PREFIX_UPPER = "P";
    private static final String ISO_DURATION_PREFIX_LOWER = "p";
    private static final String ISO_WEEK_SUFFIX = "W";
    private static final String MINUS_SIGN = "-";
    private static final String ISO8601_ALTERNATIVE_DATE_SEPARATOR = "-";

    /**
     * Matches the standard Cassandra format: an optional digit sequence followed by a unit
     * token. Units are ordered longest-first ({@code mo, ms, µs, us, ns, y, w, d, h, m, s}) to
     * avoid {@code m} consuming {@code mo}, or {@code ms} consuming {@code m}.
     */
    private static final Pattern STANDARD_PATTERN = Pattern.compile(
            "(\\d+)(" + String.join("|", UNIT_MONTH, UNIT_MILLISECOND, UNIT_MICROSECOND_MU,
                    UNIT_MICROSECOND, UNIT_NANOSECOND, UNIT_YEAR, UNIT_WEEK, UNIT_DAY, UNIT_HOUR, UNIT_MINUTE, UNIT_SECOND) + ")",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches the general ISO 8601 format, e.g. {@code P1Y2M3DT4H5M6S}. Every component is
     * optional, mirroring the driver's own grammar, so bare {@code P} or {@code PT} are valid
     * (zero-length) durations.
     */
    private static final Pattern ISO8601_PATTERN = Pattern.compile(
            ISO_DURATION_PREFIX_UPPER
                    + "(?:(\\d+)" + ISO_YEAR_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + ISO_MONTH_OR_MINUTE_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + ISO_DAY_DESIGNATOR + ")?"
                    + "(?:" + ISO_TIME_SEPARATOR
                    + "(?:(\\d+)" + ISO_HOUR_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + ISO_MONTH_OR_MINUTE_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + ISO_SECOND_DESIGNATOR + ")?)?");

    /** Matches the ISO 8601 weeks-only format, e.g. {@code P3W}. */
    private static final Pattern ISO8601_WEEK_PATTERN =
            Pattern.compile(ISO_DURATION_PREFIX_UPPER + "(\\d+)" + ISO_WEEK_SUFFIX);

    /** Matches the ISO 8601 alternative format, e.g. {@code P0001-02-03T04:05:06}. */
    private static final Pattern ISO8601_ALTERNATIVE_PATTERN = Pattern.compile(
            ISO_DURATION_PREFIX_UPPER + "(\\d{4})-(\\d{2})-(\\d{2})" + ISO_TIME_SEPARATOR + "(\\d{2}):(\\d{2}):(\\d{2})");

    /**
     * Parses a CQL duration literal into a {@link CqlDurationLiteral}. A leading {@code -} is
     * stripped and applied as a sign to all three components. The remainder is dispatched to the
     * matching pattern-specific parser by {@link #parseUnsigned}.
     *
     * @param stringDuration the duration literal to parse; must not be {@code null}, empty, or
     *                        blank
     * @return the parsed duration, decomposed into months, days and nanoseconds
     * @throws IllegalArgumentException if {@code stringDuration} is {@code null}, empty, or blank
     */
    public static CqlDurationLiteral parse(String stringDuration) {
        if (stringDuration == null) {
            throw new IllegalArgumentException("Empty duration literal");
        } else {
            String t = stringDuration.trim();
            if (t.isEmpty()) {
                throw new IllegalArgumentException("Empty duration literal");
            } else {
                boolean isNegative = t.startsWith(MINUS_SIGN);
                String unsigned = isNegative ? t.substring(1) : t;
                if (unsigned.isEmpty()) {
                    throw new IllegalArgumentException("Empty duration literal");
                } else {
                    CqlDurationLiteral magnitude = parseUnsigned(unsigned);

                    return isNegative
                            ? new CqlDurationLiteral(-magnitude.months, -magnitude.days, -magnitude.nanos)
                            : magnitude;
                }
            }
        }
    }

    /**
     * Routes to one of the four pattern-specific parsers, mirroring the format detection in the
     * driver's {@code CqlDuration.from(String)}: non-{@code P}-prefixed input is the standard
     * Cassandra format; {@code P}-prefixed input ending in {@code W} is the ISO week-only format;
     * {@code P}-prefixed input containing {@code -} is the ISO alternative format; anything else
     * {@code P}-prefixed is the general ISO 8601 format.
     */
    private static CqlDurationLiteral parseUnsigned(String unsigned) {
        if (!unsigned.startsWith(ISO_DURATION_PREFIX_UPPER) && !unsigned.startsWith(ISO_DURATION_PREFIX_LOWER)) {
            return parseStandardPattern(unsigned);
        } else {
            String upper = unsigned.toUpperCase();
            if (upper.endsWith(ISO_WEEK_SUFFIX)) {
                return parseIso8601WeekPattern(upper);
            } else if (upper.contains(ISO8601_ALTERNATIVE_DATE_SEPARATOR)) {
                return parseIso8601AlternativePattern(upper);
            } else {
                return parseIso8601Pattern(upper);
            }
        }
    }

    private static CqlDurationLiteral parseStandardPattern(String text) {
        int  months = 0;
        int  days   = 0;
        long nanos  = 0L;

        Matcher m = STANDARD_PATTERN.matcher(text);
        while (m.find()) {
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase();
            switch (unit) {
                case UNIT_YEAR:
                    months += (int) (value * MONTHS_PER_YEAR);
                    break;
                case UNIT_MONTH:
                    months += (int) value;
                    break;
                case UNIT_WEEK:
                    days += (int) (value * DAYS_PER_WEEK);
                    break;
                case UNIT_DAY:
                    days += (int) value;
                    break;
                case UNIT_HOUR:
                    nanos  += value * NANOS_PER_HOUR;
                    break;
                case UNIT_MINUTE:
                    nanos += value * NANOS_PER_MINUTE;
                    break;
                case UNIT_SECOND:
                    nanos += value * NANOS_PER_SECOND;
                    break;
                case UNIT_MILLISECOND:
                    nanos += value * NANOS_PER_MILLISECOND;
                    break;
                case UNIT_MICROSECOND:
                case UNIT_MICROSECOND_MU:
                    nanos += value * NANOS_PER_MICROSECOND;
                    break;
                case UNIT_NANOSECOND:
                    nanos  += value;
                    break;
                default:
                    break;
            }
        }
        return new CqlDurationLiteral(months, days, nanos);
    }

    private static CqlDurationLiteral parseIso8601Pattern(String isoDuration) {
        Matcher matcher = ISO8601_PATTERN.matcher(isoDuration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to parse duration literal '" + isoDuration + "'");
        } else {
            int years   = groupAsInt(matcher, 1);
            int months  = groupAsInt(matcher, 2);
            int days    = groupAsInt(matcher, 3);
            int hours   = groupAsInt(matcher, 4);
            int minutes = groupAsInt(matcher, 5);
            int seconds = groupAsInt(matcher, 6);

            long nanos = hours * NANOS_PER_HOUR + minutes * NANOS_PER_MINUTE + seconds * NANOS_PER_SECOND;
            return new CqlDurationLiteral(years * MONTHS_PER_YEAR + months, days, nanos);
        }
    }

    /** Returns the matched group as an {@code int}, or {@code 0} if the (optional) group didn't participate. */
    private static int groupAsInt(Matcher matcher, int group) {
        String value = matcher.group(group);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private static CqlDurationLiteral parseIso8601WeekPattern(String isoDuration) {
        Matcher matcher = ISO8601_WEEK_PATTERN.matcher(isoDuration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to parse duration literal '" + isoDuration + "'");
        } else {
            int weeks = Integer.parseInt(matcher.group(1));
            return new CqlDurationLiteral(0, weeks * DAYS_PER_WEEK, 0L);
        }
    }

    private static CqlDurationLiteral parseIso8601AlternativePattern(String isoDuration) {
        Matcher matcher = ISO8601_ALTERNATIVE_PATTERN.matcher(isoDuration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to parse duration literal '" + isoDuration + "'");
        } else {
            int years   = Integer.parseInt(matcher.group(1));
            int months  = Integer.parseInt(matcher.group(2));
            int days    = Integer.parseInt(matcher.group(3));
            int hours   = Integer.parseInt(matcher.group(4));
            int minutes = Integer.parseInt(matcher.group(5));
            int seconds = Integer.parseInt(matcher.group(6));

            long nanos = hours * NANOS_PER_HOUR + minutes * NANOS_PER_MINUTE + seconds * NANOS_PER_SECOND;
            return new CqlDurationLiteral(years * MONTHS_PER_YEAR + months, days, nanos);
        }
    }
}