package org.evomaster.client.java.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ISO-8601 duration string literals (e.g. {@code P1Y2M3DT4H5M6S}) into an
 * {@link IsoDuration} holding their raw, un-combined components.
 *
 * <p>Supports three formats:
 *
 * <ul>
 *   <li>General: {@code P1Y2M3DT4H5M6S}
 *   <li>Weeks only: {@code P3W}
 *   <li>Alternative: {@code P0001-02-03T04:05:06}
 * </ul>
 *
 * <p>Input must be prefixed with {@code P} or {@code p}; no sign handling is done here (callers
 * are expected to strip/apply any leading {@code -} themselves).
 */
public class IsoDurationParser {

    private IsoDurationParser() {}

    private static final char TIME_SEPARATOR = 'T';
    private static final char YEAR_DESIGNATOR = 'Y';
    private static final char MONTH_OR_MINUTE_DESIGNATOR = 'M';
    private static final char DAY_DESIGNATOR = 'D';
    private static final char HOUR_DESIGNATOR = 'H';
    private static final char SECOND_DESIGNATOR = 'S';

    private static final String DURATION_PREFIX_UPPER = "P";
    private static final String DURATION_PREFIX_LOWER = "p";
    private static final String WEEK_SUFFIX = "W";
    private static final String ALTERNATIVE_DATE_SEPARATOR = "-";

    /**
     * Matches the general format, e.g. {@code P1Y2M3DT4H5M6S}. Every component is optional,
     * so bare {@code P} or {@code PT} are valid (zero-length) durations.
     */
    private static final Pattern GENERAL_PATTERN = Pattern.compile(
            DURATION_PREFIX_UPPER
                    + "(?:(\\d+)" + YEAR_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + MONTH_OR_MINUTE_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + DAY_DESIGNATOR + ")?"
                    + "(?:" + TIME_SEPARATOR
                    + "(?:(\\d+)" + HOUR_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + MONTH_OR_MINUTE_DESIGNATOR + ")?"
                    + "(?:(\\d+)" + SECOND_DESIGNATOR + ")?)?");

    /** Matches the weeks-only format, e.g. {@code P3W}. */
    private static final Pattern WEEK_PATTERN =
            Pattern.compile(DURATION_PREFIX_UPPER + "(\\d+)" + WEEK_SUFFIX);

    /** Matches the alternative format, e.g. {@code P0001-02-03T04:05:06}. */
    private static final Pattern ALTERNATIVE_PATTERN = Pattern.compile(
            DURATION_PREFIX_UPPER + "(\\d{4})-(\\d{2})-(\\d{2})" + TIME_SEPARATOR + "(\\d{2}):(\\d{2}):(\\d{2})");

    /**
     * Parses an ISO-8601 duration literal, routing to whichever of the three supported formats
     * applies: {@code P}-prefixed input ending in {@code W} is the weeks-only format;
     * {@code P}-prefixed input containing {@code -} is the alternative format; anything else is
     * the general format.
     *
     * @param isoDuration the duration literal to parse; must be prefixed with {@code P} or {@code p}
     * @return the parsed duration, decomposed into its raw components
     * @throws IllegalArgumentException if {@code isoDuration} isn't {@code P}/{@code p}-prefixed,
     *                                   or doesn't match the format implied by its suffix/content
     */
    public static IsoDuration parse(String isoDuration) {
        if (!isoDuration.startsWith(DURATION_PREFIX_UPPER) && !isoDuration.startsWith(DURATION_PREFIX_LOWER)) {
            throw new IllegalArgumentException("Not an ISO-8601 duration literal: '" + isoDuration + "'");
        }

        String upper = isoDuration.toUpperCase();
        if (upper.endsWith(WEEK_SUFFIX)) {
            return parseWeekOnly(upper);
        } else if (upper.contains(ALTERNATIVE_DATE_SEPARATOR)) {
            return parseAlternative(upper);
        } else {
            return parseGeneral(upper);
        }
    }

    private static IsoDuration parseGeneral(String isoDuration) {
        Matcher matcher = GENERAL_PATTERN.matcher(isoDuration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to parse duration literal '" + isoDuration + "'");
        } else {
            long years = groupAsLong(matcher, 1);
            long months = groupAsLong(matcher, 2);
            long days = groupAsLong(matcher, 3);
            long hours = groupAsLong(matcher, 4);
            long minutes = groupAsLong(matcher, 5);
            long seconds = groupAsLong(matcher, 6);

            return new IsoDuration(years, months, 0, days, hours, minutes, seconds);
        }
    }

    private static IsoDuration parseWeekOnly(String isoDuration) {
        Matcher matcher = WEEK_PATTERN.matcher(isoDuration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to parse duration literal '" + isoDuration + "'");
        } else {
            long weeks = Long.parseLong(matcher.group(1));
            return new IsoDuration(0, 0, weeks, 0, 0, 0, 0);
        }
    }

    private static IsoDuration parseAlternative(String isoDuration) {
        Matcher matcher = ALTERNATIVE_PATTERN.matcher(isoDuration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to parse duration literal '" + isoDuration + "'");
        } else {
            long years = Long.parseLong(matcher.group(1));
            long months = Long.parseLong(matcher.group(2));
            long days = Long.parseLong(matcher.group(3));
            long hours = Long.parseLong(matcher.group(4));
            long minutes = Long.parseLong(matcher.group(5));
            long seconds = Long.parseLong(matcher.group(6));

            return new IsoDuration(years, months, 0, days, hours, minutes, seconds);
        }
    }

    /** Returns the matched group as a {@code long}, or {@code 0} if the (optional) group didn't participate. */
    private static long groupAsLong(Matcher matcher, int group) {
        String value = matcher.group(group);
        return value == null ? 0 : Long.parseLong(value);
    }
}