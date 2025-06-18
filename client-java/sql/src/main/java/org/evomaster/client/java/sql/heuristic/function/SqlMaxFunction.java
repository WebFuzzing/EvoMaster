package org.evomaster.client.java.sql.heuristic.function;

import java.util.List;

public class SqlMaxFunction extends SqlAggregateFunction {

    private static final String MAX_FUNCTION_NAME = "MAX";

    public SqlMaxFunction() {
        super(MAX_FUNCTION_NAME);
    }

    @Override
    public Object evaluateValues(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }

        Object maxValue = null;
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (!(value instanceof Comparable)) {
                throw new IllegalArgumentException("Cannot compare values of type " + value.getClass().getName());
            }

            if (maxValue == null || ((Comparable) value).compareTo(maxValue) > 0) {
                maxValue = value;
            }
        }
        return maxValue;

    }
}
