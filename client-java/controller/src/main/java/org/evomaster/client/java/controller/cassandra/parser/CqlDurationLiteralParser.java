package org.evomaster.client.java.controller.cassandra.parser;

import org.evomaster.client.java.controller.cassandra.model.CqlDurationLiteral;
import org.evomaster.client.java.utils.IsoDuration;
import org.evomaster.client.java.utils.IsoDurationParser;

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

    private static final String ISO_DURATION_PREFIX_UPPER = "P";
    private static final String ISO_DURATION_PREFIX_LOWER = "p";
    private static final String MINUS_SIGN = "-";

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
     * Routes to either the standard Cassandra format parser or, for {@code P}/{@code p}-prefixed
     * input, delegates to {@link IsoDurationParser} for the three ISO-8601 duration formats,
     * mirroring the format detection in the driver's {@code CqlDuration.from(String)}.
     */
    private static CqlDurationLiteral parseUnsigned(String unsigned) {
        if (!unsigned.startsWith(ISO_DURATION_PREFIX_UPPER) && !unsigned.startsWith(ISO_DURATION_PREFIX_LOWER)) {
            return parseStandardPattern(unsigned);
        } else {
            return toCqlDurationLiteral(IsoDurationParser.parse(unsigned));
        }
    }

    /**
     * Combines the raw ISO-8601 components into Cassandra's specific months/days/nanos
     * decomposition: years are folded into months, weeks into days, and hours/minutes/seconds
     * into a single nanosecond count.
     */
    private static CqlDurationLiteral toCqlDurationLiteral(IsoDuration iso) {
        int months = (int) (iso.years * MONTHS_PER_YEAR + iso.months);
        int days = (int) (iso.weeks * DAYS_PER_WEEK + iso.days);
        long nanos = iso.hours * NANOS_PER_HOUR + iso.minutes * NANOS_PER_MINUTE + iso.seconds * NANOS_PER_SECOND;
        return new CqlDurationLiteral(months, days, nanos);
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
}