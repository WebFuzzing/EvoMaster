package org.evomaster.client.java.sql.heuristic.function;

import java.util.List;

public class SqlMinFunction extends SqlAggregateFunction {

    private static final String MIN_FUNCTION_NAME = "MIN";

    public SqlMinFunction() {
        super(MIN_FUNCTION_NAME);
    }

    @Override
    public Object evaluateValues(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }

        Object minValue = null;
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (!(value instanceof Comparable)) {
                throw new IllegalArgumentException("Cannot compare values of type " + value.getClass().getName());
            }

            if (minValue == null || ((Comparable) value).compareTo(minValue) < 0) {
                minValue = value;
            }
        }
        return minValue;
    }
}
