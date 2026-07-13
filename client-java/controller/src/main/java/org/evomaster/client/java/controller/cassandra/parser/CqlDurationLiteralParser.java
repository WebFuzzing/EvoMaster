package org.evomaster.client.java.controller.cassandra.parser;

import org.evomaster.client.java.controller.cassandra.model.CqlDurationLiteral;

import java.time.Duration;
import java.time.Period;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Cassandra duration string literal into a {@link CqlDurationLiteral}.
 *
 * Supports two formats:
 *   - ISO 8601:          P1Y2M3DT4H5M6S
 *   - Standard Cassandra: 1y2mo3d4h5m6s  (units may appear in any order)
 */
public class CqlDurationLiteralParser {

    private CqlDurationLiteralParser() {}

    private static final String UNIT_MONTH = "mo";
    private static final String UNIT_MILLISECOND = "ms";
    private static final String UNIT_MICROSECOND_MU = "µs";
    private static final String UNIT_MICROSECOND = "us";
    private static final String UNIT_NANOSECOND = "ns";
    private static final String UNIT_YEAR = "y";
    private static final String UNIT_DAY = "d";
    private static final String UNIT_HOUR = "h";
    private static final String UNIT_MINUTE = "m";
    private static final String UNIT_SECOND = "s";

    /**
     * Matches an optional digit sequence followed by a unit token. Units are ordered
     * longest-first ({@code mo, ms, µs, us, ns, y, d, h, m, s}) to avoid {@code m} consuming
     * {@code mo}, or {@code ms} consuming {@code m}.
     */
    private static final Pattern TOKEN = Pattern.compile(
            "(\\d+)(" + String.join("|", UNIT_MONTH, UNIT_MILLISECOND, UNIT_MICROSECOND_MU,
                    UNIT_MICROSECOND, UNIT_NANOSECOND, UNIT_YEAR, UNIT_DAY, UNIT_HOUR, UNIT_MINUTE, UNIT_SECOND) + ")",
            Pattern.CASE_INSENSITIVE);

    private static final int  MONTHS_PER_YEAR = 12;
    private static final long NANOS_PER_HOUR = 3_600_000_000_000L;
    private static final long NANOS_PER_MINUTE = 60_000_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final long NANOS_PER_MICROSECOND = 1_000L;

    private static final char ISO_TIME_SEPARATOR = 'T';

    private static final String ISO_DURATION_PREFIX_UPPER = "P";
    private static final String ISO_DURATION_PREFIX_LOWER = "p";

    /**
     * Parses a CQL duration literal into a {@link CqlDurationLiteral}, auto-detecting the
     * format: strings starting with {@code P}/{@code p} are parsed as ISO 8601 (e.g.
     * {@code P1Y2M3DT4H5M6S}), everything else as the standard Cassandra format (e.g.
     * {@code 1y2mo3d4h5m6s}).
     *
     * @param stringDuration the duration literal to parse; must not be {@code null} or empty
     * @return the parsed duration, decomposed into months, days and nanoseconds
     * @throws IllegalArgumentException if {@code stringDuration} is {@code null} or empty
     */
    public static CqlDurationLiteral parse(String stringDuration) {
        if (stringDuration == null || stringDuration.isEmpty()) {
            throw new IllegalArgumentException("Empty duration literal");
        }

        String t = stringDuration.trim();
        if (t.startsWith(ISO_DURATION_PREFIX_UPPER) || t.startsWith(ISO_DURATION_PREFIX_LOWER)) {
            return parseIso(t);
        }

        return parseCassandra(t);
    }

    private static CqlDurationLiteral parseIso(String isoDuration) {
        // Split on T (or t) to separate date and time parts
        String upper = isoDuration.toUpperCase();
        int tIndex = upper.indexOf(ISO_TIME_SEPARATOR);

        int months = 0;
        int days   = 0;
        long nanos = 0L;

        if (tIndex < 0) {
            // Date part only: P1Y2M3D
            Period p = Period.parse(upper);
            months = p.getYears() * 12 + p.getMonths();
            days   = p.getDays();
        } else {
            String datePart = upper.substring(0, tIndex);   // e.g. "P1Y2M3D"
            String timePart = upper.substring(tIndex);      // e.g. "T4H5M6S"

            if (datePart.length() > 1) {
                Period p = Period.parse(datePart);
                months = p.getYears() * 12 + p.getMonths();
                days   = p.getDays();
            }
            if (timePart.length() > 1) {
                Duration d = Duration.parse(ISO_DURATION_PREFIX_UPPER + timePart);
                nanos = d.toNanos();
            }
        }
        return new CqlDurationLiteral(months, days, nanos);
    }

    private static CqlDurationLiteral parseCassandra(String text) {
        int  months = 0;
        int  days   = 0;
        long nanos  = 0L;

        Matcher m = TOKEN.matcher(text);
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