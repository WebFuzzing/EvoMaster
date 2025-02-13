package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * created by manzhang on 2024/7/29
 */
public class ColumnTypeParser {

    //https://dev.mysql.com/doc/refman/8.0/en/date-and-time-type-syntax.html
    private final static String[] DATE_FORMATS  = {"yyyy-MM-dd HH:mm:ss.SSSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "HH:mm:ss.SSSS", "HH:mm:ss"};

    public static Instant getAsInstant(String content) {

        List<Function<String, Instant>> parsers = Arrays.asList(
                s -> ZonedDateTime.parse(s).toInstant(),
                Instant::parse,
                s -> OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")).toInstant(),
                s -> OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSX")).toInstant(),
                s -> {
                        /*
                           maybe it is in some weird format like 28-Feb-17...
                           this shouldn't really happen, but looks like Hibernate generate SQL from
                           JPQL with Date handled like this :(
                        */
                    DateTimeFormatter df = new DateTimeFormatterBuilder()
                            // case insensitive to parse JAN and FEB
                            .parseCaseInsensitive()
                            // add pattern
                            .appendPattern("dd-MMM-yy")
                            // create formatter (use English Locale to parse month names)
                            .toFormatter(Locale.ENGLISH);

                    return LocalDate.parse(content.toString(), df)
                            .atStartOfDay().toInstant(ZoneOffset.UTC);
                },
                s -> parseDate(s)
        );


            /*
                Dealing with timestamps is a mess, including bugs in the JDK itself...
                https://stackoverflow.com/questions/43360852/cannot-parse-string-in-iso-8601-format-lacking-colon-in-offset-to-java-8-date
                So, here we try different date parsers, hoping at least one will work...
             */

        for (Function<String, Instant> p : parsers) {
            try {
                return p.apply(content);
            } catch (DateTimeParseException t) {
                // Do nothing
            }
        }

        SimpleLogger.warn("Cannot handle time value in the format: " + content);
        return null;
    }

    private static Instant parseDate(String str){

        for (String dformat : DATE_FORMATS){
            try {
                return new SimpleDateFormat(dformat).parse(str).toInstant();
            } catch (ParseException ex) {
//                throw new DateTimeParseException("Cannot parse to yyyy-MM-dd HH:mm:ss.SSSS, yyyy-MM-dd HH:mm:ss", str, ex.getErrorOffset(), ex);
            }
        }
        throw new IllegalArgumentException("Cannot parse to yyyy-MM-dd HH:mm:ss.SSSS, yyyy-MM-dd HH:mm:ss");
    }
}
