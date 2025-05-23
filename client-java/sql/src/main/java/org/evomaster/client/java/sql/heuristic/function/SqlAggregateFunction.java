package org.evomaster.client.java.sql.heuristic.function;

import java.math.BigDecimal;
import java.util.List;

public abstract class SqlAggregateFunction extends SqlFunction {

    public SqlAggregateFunction(String functionName) {
        super(functionName);
    }

    @Override
    public final Object evaluate(Object... arguments) {
        if (arguments.length == 0) {
            return null;
        } else if (arguments[0] instanceof List<?>) {
            List<Object> values = (List<Object>) arguments[0];
            return evaluateValues(values);
        } else {
            throw new IllegalArgumentException("Invalid argument type: " + arguments[0].getClass().getName());
        }
    }

    protected abstract Object evaluateValues(List<Object> values);



}
