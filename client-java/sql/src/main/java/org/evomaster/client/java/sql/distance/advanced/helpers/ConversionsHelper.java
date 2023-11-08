package org.evomaster.client.java.sql.distance.advanced.helpers;

public class ConversionsHelper {

    public static Double convertToDouble(Object object) {
        if(object instanceof Double) {
            return (Double) object;
        } else if(object instanceof Number) {
            return ((Number) object).doubleValue();
        } else {
            throw new AssertionError("Type must be number");
        }
    }

    public static Boolean convertToBoolean(Object object) {
        if(object instanceof Boolean) {
            return (Boolean) object;
        } else if(object instanceof Number) {
            return convertToBoolean((Number) object);
        } else {
            throw new AssertionError("Type must be boolean or number");
        }
    }

    public static Boolean convertToBoolean(Number number) {
        return convertToDouble(number) != 0;
    }
}
