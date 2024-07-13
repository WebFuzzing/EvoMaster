package org.evomaster.client.java.sql.advanced.helpers;

import com.github.sisyphsu.dateparser.DateParserUtils;
import org.evomaster.client.java.distance.heuristics.Truthness;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static java.util.Objects.nonNull;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.Truthness.TRUE;

public class ConversionsHelper {

    public static Date convertToDate(Object object) {
        if(nonNull(object)) {
            return convertToNonNullDate(object);
        } else {
            return null;
        }
    }

    private static Date convertToNonNullDate(Object object) {
        if(object instanceof Date) {
            if(object instanceof Time) {
                return convertToDate(object.toString());
            } else {
                return (Date) object;
            }
        } else if(object instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) object).atZone(ZoneId.systemDefault()).toInstant());
        } else if(object instanceof String) {
            return DateParserUtils.parseDate((String) object);
        } else {
            throw new RuntimeException("Type must be date, local date time or string");
        }
    }

    public static Double convertToDouble(Object object) {
        if(nonNull(object)) {
            return convertToNonNullDouble(object);
        } else {
            return null;
        }
    }

    private static Double convertToNonNullDouble(Object object) {
        if(object instanceof Double) {
            return (Double) object;
        } else if(object instanceof Number) {
            return ((Number) object).doubleValue();
        } else if(object instanceof Boolean) {
            return (Boolean) object ? 1d : 0d;
        } else if(object instanceof Date) {
            return convertToDouble(((Date) object).getTime());
        } else {
            throw new RuntimeException("Type must be number, boolean or date");
        }
    }

    public static Boolean convertToBoolean(Object object) {
        if(nonNull(object)) {
            return convertToNonNullBoolean(object);
        } else {
            return null;
        }
    }

    private static Boolean convertToNonNullBoolean(Object object) {
        if(object instanceof Boolean) {
            return (Boolean) object;
        } else if(object instanceof Number) {
            return convertToNonNullDouble(object) != 0;
        } else {
            throw new RuntimeException("Type must be boolean or number");
        }
    }

    public static Truthness convertToTruthness(Object object) {
        if(nonNull(object)) {
            return convertToNonNullTruthness(object);
        } else {
            return FALSE;
        }
    }

    private static Truthness convertToNonNullTruthness(Object object) {
        if(object instanceof Truthness) {
            return (Truthness) object;
        } else if(object instanceof Boolean || object instanceof Number) {
            return convertToNonNullBoolean(object) ? TRUE : FALSE;
        } else {
            throw new RuntimeException("Type must be truthness, boolean or number");
        }
    }
}
