package org.evomaster.client.java.sql.heuristic.function;

import org.evomaster.client.java.sql.internal.ColumnTypeParser;

import java.time.*;
import java.time.temporal.TemporalAccessor;

public class DateTrunc extends SqlFunction {

    public static final String MONTH = "month";
    public static final String HH = "hh";
    public static final String HOUR = "hour";
    public static final String DD = "dd";
    public static final String DAY = "day";
    public static final String SECOND = "second";
    public static final String MINUTE = "minute";
    public static final String YEAR = "year";

    private static final String DATE_TRUNC = "DATE_TRUNC";

    public DateTrunc() {
        super(DATE_TRUNC);
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments == null || arguments.length != 2) {
            throw new IllegalArgumentException("DATE_TRUNC requires two arguments");
        }
        final String unit = String.valueOf(arguments[0]).toLowerCase();
        final Object tsObj = arguments[1];
        if (tsObj == null) {
            return null;
        }

        if (tsObj instanceof String) {
            Instant instant = ColumnTypeParser.getAsInstant((String) tsObj);
            return truncateInstant(instant, unit);
        } else if (tsObj instanceof java.util.Date) {
            // legacy datetime API from Java <8
            Instant instant = ((java.util.Date) tsObj).toInstant();
            Instant truncatedInstant = truncateInstant(instant, unit);
            return java.util.Date.from(truncatedInstant);
        } else if (tsObj instanceof TemporalAccessor) {
            // java.time API from Java 8+
            return truncateTemporalAccessor((TemporalAccessor) tsObj, unit);
        } else {
            throw new IllegalArgumentException("Unsupported timestamp object type: " + tsObj.getClass());
        }
    }

    private static TemporalAccessor truncateTemporalAccessor(TemporalAccessor t, String unit) {
        unit = unit.toLowerCase();

        // Determine what actual type we are working with
        if (t instanceof LocalDateTime) {
            return truncateLocalDateTime((LocalDateTime) t, unit);
        }
        if (t instanceof LocalDate) {
            return truncateLocalDate((LocalDate) t, unit);
        }
        if (t instanceof LocalTime) {
            return truncateLocalTime((LocalTime) t, unit);
        }
        if (t instanceof OffsetDateTime) {
            return truncateOffsetDateTime((OffsetDateTime) t, unit);
        }
        if (t instanceof ZonedDateTime) {
            return truncateZonedDateTime((ZonedDateTime) t, unit);
        }
        if (t instanceof OffsetTime) {
            return truncateOffsetTime((OffsetTime) t, unit);
        }
        if (t instanceof Instant) {
            return truncateInstant((Instant) t, unit);
        }

        throw new UnsupportedOperationException(
                "Unsupported TemporalAccessor type: " + t.getClass());
    }

    private static Instant truncateInstant(Instant t, String unit) {
        // convert to UTC LocalDateTime, truncate, and convert back
        LocalDateTime ldt = LocalDateTime.ofInstant(t, ZoneOffset.UTC);
        LocalDateTime truncated = truncateLocalDateTime(ldt, unit);
        return truncated.toInstant(ZoneOffset.UTC);
    }

    // ---- Concrete truncators -------------------------------------------------

    private static LocalDateTime truncateLocalDateTime(LocalDateTime dt, String unit) {
        switch (unit) {
            case SECOND:
                return dt.withNano(0);
            case MINUTE:
                return dt.withSecond(0).withNano(0);
            case HOUR:
                return dt.withMinute(0).withSecond(0).withNano(0);
            case DAY:
                return dt.toLocalDate().atStartOfDay();
            case MONTH:
                return LocalDate.of(dt.getYear(), dt.getMonth(), 1).atStartOfDay();
            case YEAR:
                return LocalDate.of(dt.getYear(), 1, 1).atStartOfDay();
            default:
                throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
    }

    private static LocalDate truncateLocalDate(LocalDate d, String unit) {
        switch (unit) {
            case DAY:
                return d;
            case MONTH:
                return d.withDayOfMonth(1);
            case YEAR:
                return d.withDayOfYear(1);
            default:
                throw new IllegalArgumentException("Unsupported unit for LocalDate: " + unit);
        }
    }

    private static LocalTime truncateLocalTime(LocalTime t, String unit) {
        switch (unit) {
            case SECOND:
                return t.withNano(0);
            case MINUTE:
                return t.withSecond(0).withNano(0);
            case HOUR:
                return t.withMinute(0).withSecond(0).withNano(0);
            default:
                throw new IllegalArgumentException("Unsupported unit for LocalTime: " + unit);
        }
    }

    private static OffsetDateTime truncateOffsetDateTime(OffsetDateTime dt, String unit) {
        LocalDateTime truncated = truncateLocalDateTime(dt.toLocalDateTime(), unit);
        return truncated.atOffset(dt.getOffset());
    }

    private static OffsetTime truncateOffsetTime(OffsetTime t, String unit) {
        LocalTime truncated = truncateLocalTime(t.toLocalTime(), unit);
        return truncated.atOffset(t.getOffset());
    }

    private static ZonedDateTime truncateZonedDateTime(ZonedDateTime dt, String unit) {
        LocalDateTime truncated = truncateLocalDateTime(dt.toLocalDateTime(), unit);
        return truncated.atZone(dt.getZone());
    }
}
