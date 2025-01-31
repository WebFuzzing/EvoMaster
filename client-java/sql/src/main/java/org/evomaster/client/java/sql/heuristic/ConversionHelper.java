package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.internal.ColumnTypeParser;

import java.sql.Time;
import java.time.*;

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
        if (object ==null) {
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
}
