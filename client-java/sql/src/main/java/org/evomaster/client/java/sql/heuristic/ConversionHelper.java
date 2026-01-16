package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.internal.ColumnTypeParser;

import java.sql.Time;
import java.time.*;
import java.util.Date;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.FALSE_TRUTHNESS;

/**
 * Utility class for converting various date/time objects to {@link Instant}.
 */
public class ConversionHelper {

    /**
     * Converts an object to an {@link Instant}.
     * The object can be of type {@link Time}, {@link java.sql.Date}, {@link java.util.Date},
     * {@link OffsetDateTime}, {@link OffsetTime}, {@link Long}, or {@link String}.
     *
     * @param object the object to convert
     * @return the converted {@link Instant}, or null if the input object is null
     * @throws IllegalArgumentException if the object is not a supported type
     */
    public static Instant convertToInstant(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof Time) {
            String timeAsString = object.toString();
            return ColumnTypeParser.getAsInstant(timeAsString);
        } else if (object instanceof java.sql.Date) {
            String dateAsString = object.toString();
            return ColumnTypeParser.getAsInstant(dateAsString);
        } else if (object instanceof java.util.Date) {
            return ((java.util.Date) object).toInstant();
        } else if (object instanceof OffsetDateTime) {
            return ((OffsetDateTime) object).toInstant();
        } else if (object instanceof OffsetTime) {
            OffsetTime offsetTime = (OffsetTime) object;
            LocalDate localDate = LocalDate.of(1970, 1, 1);
            LocalDateTime localDateTime = LocalDateTime.of(localDate, offsetTime.toLocalTime());
            OffsetDateTime offsetDateTime = localDateTime.atOffset(offsetTime.getOffset());
            return offsetDateTime.toInstant();
        } else if (object instanceof Long) {
            Long year = (Long) object;
            String yearAsDate = year + "-01-01";
            return ColumnTypeParser.getAsInstant(yearAsDate);
        } else if (object instanceof String) {
            String objectAsString = (String) object;
            return ColumnTypeParser.getAsInstant(objectAsString);
        } else {
            throw new IllegalArgumentException("Argument must be date, local date time or string but got " + object.getClass().getName());
        }
    }

    public static Double convertToDouble(Object object) {
        if (nonNull(object)) {
            return convertToNonNullDouble(object);
        } else {
            return null;
        }
    }

    private static Double convertToNonNullDouble(Object object) {
        if (object instanceof Double) {
            return (Double) object;
        } else if (object instanceof Number) {
            return ((Number) object).doubleValue();
        } else if (object instanceof Boolean) {
            return (Boolean) object ? 1d : 0d;
        } else if (object instanceof Date) {
            return convertToDouble(((Date) object).getTime());
        } else {
            throw new RuntimeException("Type must be number, boolean or date");
        }
    }

    private static Boolean convertToNonNullBoolean(Object object) {
        if (object instanceof Boolean) {
            return (Boolean) object;
        } else if (object instanceof Number) {
            return convertToNonNullDouble(object) != 0;
        } else {
            throw new RuntimeException("Type must be boolean or number");
        }
    }

    private static Truthness convertToNonNullTruthness(Object object) {
        if (object instanceof Truthness) {
            return (Truthness) object;
        } else if (object instanceof Boolean || object instanceof Number) {
            return convertToNonNullBoolean(object) ? SqlHeuristicsCalculator.TRUE_TRUTHNESS : FALSE_TRUTHNESS;
        } else {
            throw new RuntimeException("Type must be truthness, boolean or number");
        }
    }

    public static Truthness convertToTruthness(Object object) {
        if (nonNull(object)) {
            return convertToNonNullTruthness(object);
        } else {
            return SqlHeuristicsCalculator.FALSE_TRUTHNESS;
        }
    }

    public static Boolean convertToBoolean(Object object) {
        if(nonNull(object)) {
            return convertToNonNullBoolean(object);
        } else {
            return null;
        }
    }

    public static UUID convertToUUID(Object object) {
        if (nonNull(object)) {
            return convertToNonNullUUID(object);
        } else {
            return null;
        }
    }

    private static UUID convertToNonNullUUID(Object object) {
        if (object instanceof UUID) {
            return (UUID) object;
        } else if (object instanceof String){
            return UUID.fromString((String) object);
        } else {
            throw new IllegalArgumentException("Type must be UUID or string");
        }
    }
}
