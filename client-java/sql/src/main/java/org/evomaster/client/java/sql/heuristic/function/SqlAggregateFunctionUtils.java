package org.evomaster.client.java.sql.heuristic.function;

import java.math.BigDecimal;
import java.util.List;

public class SqlAggregateFunctionUtils {

    static BigDecimal sumOfBigDecimals(List<Object> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Object value : values) {
            if (value != null) {
                if (value instanceof BigDecimal) {
                    sum = sum.add((BigDecimal) value);
                } else {
                    throw new IllegalArgumentException("Cannot sum values of type " + value.getClass().getName());
                }
            }
        }
        return sum;
    }

    static long sumOfLongValues(List<Object> values) {
        long sum = 0L;
        for (Object value : values) {
            if (value != null) {
                if (value instanceof Long) {
                    sum += (Long) value;
                } else if (value instanceof Integer) {
                    sum += ((Integer) value).longValue();
                } else if (value instanceof Short) {
                    sum += ((Short) value).longValue();
                } else if (value instanceof Byte) {
                    sum += ((Byte) value).longValue();
                } else {
                    throw new IllegalArgumentException("Cannot sum values of type " + value.getClass().getName());
                }
            }
        }
        return sum;
    }

    static double sumOfDoubleValues(List<Object> values) {
        double sum = 0.0;
        for (Object value : values) {
            if (value != null) {
                if (value instanceof Double) {
                    sum += (Double) value;
                } else if (value instanceof Float) {
                    sum += ((Float) value).doubleValue();
                } else {
                    throw new IllegalArgumentException("Cannot sum values of type " + value.getClass().getName());
                }
            }
        }
        return sum;
    }

    static boolean allFloats(List<Object> values) {
        return allMatchType(values, Float.class);
    }

    static <T extends Number> boolean allMatchType(List<Object> values, Class<T> type) {
        return values.stream()
                .filter(v -> (v != null))
                .allMatch(type::isInstance);
    }

    static boolean allDoubles(List<Object> values) {
        return allMatchType(values, Double.class);
    }

    static boolean allBigDecimals(List<Object> values) {
        return allMatchType(values, BigDecimal.class);
    }

    static boolean allNullValues(List<Object> values) {
        return countNonNullValues(values) == 0;
    }

    static long countNonNullValues(List<Object> values) {
        return values.stream()
                .filter(v -> (v != null))
                .count();
    }
}
