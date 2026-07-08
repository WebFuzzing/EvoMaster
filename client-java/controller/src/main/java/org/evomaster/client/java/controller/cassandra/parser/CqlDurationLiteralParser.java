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

    /**
     * Matches an optional digit sequence followed by a unit token. Units are ordered
     * longest-first ({@code mo, ms, us, µs, ns, y, d, h, m, s}) to avoid {@code m} consuming
     * {@code mo}, or {@code ms} consuming {@code m}.
     */
    private static final Pattern TOKEN =
            Pattern.compile("(\\d+)(mo|ms|µs|us|ns|y|d|h|m|s)", Pattern.CASE_INSENSITIVE);

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
        if (t.startsWith("P") || t.startsWith("p")) {
            return parseIso(t);
        }

        return parseCassandra(t);
    }

    private static CqlDurationLiteral parseIso(String isoDuration) {
        // Split on T (or t) to separate date and time parts
        String upper = isoDuration.toUpperCase();
        int tIndex = upper.indexOf('T');

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
                Duration d = Duration.parse("P" + timePart);
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
                case "y":  months += (int)(value * 12);                    break;
                case "mo": months += (int) value;                          break;
                case "d":  days   += (int) value;                          break;
                case "h":  nanos  += value * 3_600_000_000_000L;           break;
                case "m":  nanos  += value * 60_000_000_000L;              break;
                case "s":  nanos  += value * 1_000_000_000L;               break;
                case "ms": nanos  += value * 1_000_000L;                   break;
                case "us":
                case "µs": nanos  += value * 1_000L;                       break;
                case "ns": nanos  += value;                                 break;
                default: break;
            }
        }
        return new CqlDurationLiteral(months, days, nanos);
    }
}