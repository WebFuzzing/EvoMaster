package org.evomaster.client.java.controller.redis;

/**
 * A numeric range filter, e.g. {@code @age:[18 65]}. Both bounds are inclusive and finite;
 * open-ended ({@code +inf}/{@code -inf}) and exclusive (leading {@code (}) ranges are not
 * part of the supported grammar.
 */
public class RedisSearchNumericFilter implements RedisSearchFilter {

    private final String fieldName;
    private final double min;
    private final double max;

    public RedisSearchNumericFilter(String fieldName, double min, double max) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not be greater than max (" + max + ")");
        }
        this.fieldName = fieldName;
        this.min = min;
        this.max = max;
    }

    public String getFieldName() {
        return fieldName;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
