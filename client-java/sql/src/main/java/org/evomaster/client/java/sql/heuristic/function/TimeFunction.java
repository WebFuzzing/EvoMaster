package org.evomaster.client.java.sql.heuristic.function;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

public class TimeFunction extends SqlFunction {

    public TimeFunction() {
        super("TIME");
    }

    @Override
    public Object evaluate(Object... arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("TIME() function takes exactly one argument");
        }
        Object argument = arguments[0];
        if (argument instanceof Date) {
            Date date = (Date) argument;
            return timeOf(date);
        } else if (argument instanceof String) {
            String s = (String) argument;
            try {
                Date date = Timestamp.valueOf(s);
                return timeOf(date);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid string format for TIME() function: " + s);
            }
        }
        throw new UnsupportedOperationException("Implement this");
    }

    private Time timeOf(Date date) {
        return new Time(date.getTime());
    }

}
