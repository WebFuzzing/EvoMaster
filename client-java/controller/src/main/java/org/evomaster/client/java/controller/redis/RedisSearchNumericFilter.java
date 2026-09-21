package org.evomaster.client.java.controller.redis;

/**
 * A numeric range filter, e.g. {@code @age:[18 65]}. Both bounds are inclusive and finite;
 * open-ended ({@code +inf}/{@code -inf}) and exclusive (leading {@code (}) ranges are not
 * part of the supported grammar.
 */
public class RedisSearchNumericFilter implements RedisSearchFilter {

    private final String field;
    private final double min;
    private final double max;

    public RedisSearchNumericFilter(String field, double min, double max) {
        this.field = field;
        this.min = min;
        this.max = max;
    }

    public String getField() {
        return field;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
