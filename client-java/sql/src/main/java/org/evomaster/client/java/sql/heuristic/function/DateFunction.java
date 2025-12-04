package org.evomaster.client.java.sql.heuristic.function;

import org.evomaster.client.java.sql.internal.SqlDateTimeParser;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;

/**
 * Represents the SQL DATE() function, which converts a given value into a SQL Date object.
 *
 * The DATE() function accepts a single argument and attempts to convert it into a SQL Date.
 *
 * It supports the following types of inputs:
 * - {@link Date}: Returns the argument as it is since it is already a valid SQL Date.
 * - {@link Timestamp}: Converts the timestamp to a date by extracting the date part.
 * - {@link java.util.Date}: Converts the date into a SQL Date.
 * - {@link String}: Attempts to parse the string using known SQL date/time formats. Returns a SQL Date if parsing succeeds.
 *
 * When the input value is null, the function returns null, maintaining SQL semantics.
 *
 * If the input type is unsupported or the string cannot be parsed into a valid date, an {@link IllegalArgumentException} is thrown.
 */
public class DateFunction extends SqlFunction {


    public DateFunction() {
        super("DATE");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("DATE() function takes exactly one argument");
        }

        Object arg = arguments[0];
        if (arg == null) {
            return null;  // SQL DATE(NULL) → NULL
        }

        // --- java.sql.Date ---
        if (arg instanceof Date) {
            return arg;  // Already a pure DATE
        }

        // --- java.sql.Timestamp ---
        if (arg instanceof Timestamp) {
            Timestamp ts = (Timestamp) arg;
            LocalDate d = ts.toLocalDateTime().toLocalDate();
            return Date.valueOf(d);
        }

        // --- java.util.Date (includes java.sql.Time) ---
        if (arg instanceof java.util.Date) {
            java.util.Date d = (java.util.Date) arg;
            Instant i = Instant.ofEpochMilli(d.getTime());
            LocalDate ld = i.atZone(ZoneId.systemDefault()).toLocalDate();
            return Date.valueOf(ld);
        }

        // --- String: parse with multiple fallback formats ---
        if (arg instanceof String) {
            String s = (String) arg;
            s = s.trim();
            if (s.isEmpty()) {
                return null;
            }

            SqlDateTimeParser parser = new SqlDateTimeParser();
            return parser.parseDate(s);
        }

        throw new IllegalArgumentException(
                "Unsupported type for DATE(): " + arg.getClass().getName()
        );
    }

}
