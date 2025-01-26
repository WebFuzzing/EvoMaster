package org.evomaster.client.java.sql.internal;

import java.sql.Time;
import java.time.*;

public class ConversionHelper {

    public static Instant convertToInstant(Object object) {
        if (object ==null) {
            return null;
        } else if (object instanceof Time) {
            String timeAsString = object.toString();
            return ColumnTypeParser.getAsInstant(timeAsString);
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
