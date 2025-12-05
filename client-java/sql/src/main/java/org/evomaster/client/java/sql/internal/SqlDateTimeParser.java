package org.evomaster.client.java.sql.internal;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class SqlDateTimeParser {

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,                      // yyyy-MM-dd
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,                 // yyyy-MM-ddTHH:mm:ss
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),    // MySQL classic format
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")        // Fallback
    };

    public Date parseDate(String s) {
        Objects.requireNonNull(s);

        // Try each known SQL format
        for (DateTimeFormatter f : DATE_FORMATS) {
            final Date ld = parseDate(s, f);
            if (ld != null) {
                return ld;
            }

            final Date ldt = parseDateTime(s, f);
            if (ldt != null) {
                return ldt;
            }
        }
        throw new IllegalArgumentException("Cannot parse DATE(): unsupported date format: " + s);
    }

    private static Date parseDateTime(String s, DateTimeFormatter f) {
        try {
            // Try as LocalDateTime
            LocalDateTime ldt = LocalDateTime.parse(s, f);
            return Date.valueOf(ldt.toLocalDate());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static Date parseDate(String s, DateTimeFormatter f) {
        try {
            // Try as LocalDate
            LocalDate ld = LocalDate.parse(s, f);
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}
