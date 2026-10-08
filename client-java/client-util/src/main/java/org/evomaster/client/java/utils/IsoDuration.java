package org.evomaster.client.java.utils;

/**
 * The raw, un-combined components of a parsed ISO-8601 duration string, as produced by
 * {@link IsoDurationParser}. Fields that didn't participate in the match default to {@code 0}.
 */
public class IsoDuration {

    public final long years;
    public final long months;
    public final long weeks;
    public final long days;
    public final long hours;
    public final long minutes;
    public final long seconds;

    public IsoDuration(long years, long months, long weeks, long days, long hours, long minutes, long seconds) {
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }
}