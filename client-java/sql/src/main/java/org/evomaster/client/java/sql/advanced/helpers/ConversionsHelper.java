package org.evomaster.client.java.sql.advanced.helpers;

import com.github.sisyphsu.dateparser.DateParserUtils;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class ConversionsHelper {

    public static Date convertToDate(Object object) {
        if(object instanceof Date) {
            if(object instanceof Time) {
                return convertToDate(object.toString());
            } else {
                return (Date) object;
            }
        } else if(object instanceof LocalDateTime) {
            return convertToDate((LocalDateTime) object);
        } else if(object instanceof String) {
            return convertToDate((String) object);
        } else {
            throw new RuntimeException("Type must be date, local date time or string");
        }
    }

    public static Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date convertToDate(String string) {
        return DateParserUtils.parseDate(string);
    }

    public static Double convertToDouble(Object object) {
        if(object instanceof Double) {
            return (Double) object;
        } else if(object instanceof Number) {
            return convertToDouble((Number) object);
        } else if(object instanceof Boolean) {
            return convertToDouble((Boolean) object);
        } else if(object instanceof Date) {
            return convertToDouble((Date) object);
        } else {
            throw new RuntimeException("Type must be number, boolean or date");
        }
    }

    public static Double convertToDouble(Number number) {
        return number.doubleValue();
    }

    public static Double convertToDouble(Boolean booleanValue) {
        return booleanValue ? 1d : 0d;
    }

    public static Double convertToDouble(Date date) {
        return convertToDouble(date.getTime());
    }

    public static Boolean convertToBoolean(Object object) {
        if(object instanceof Boolean) {
            return (Boolean) object;
        } else if(object instanceof Number) {
            return convertToBoolean((Number) object);
        } else {
            throw new RuntimeException("Type must be boolean or number");
        }
    }

    public static Boolean convertToBoolean(Number number) {
        return convertToDouble(number) != 0;
    }
}
