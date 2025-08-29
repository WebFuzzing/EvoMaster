package org.evomaster.client.java.sql.heuristic.function;

import java.util.List;

import static org.evomaster.client.java.sql.heuristic.function.SqlAggregateFunctionUtils.*;

public class SqlSumFunction extends SqlAggregateFunction {

    private static final String SUM_FUNCTION_NAME = "SUM";

    public SqlSumFunction() {
        super(SUM_FUNCTION_NAME);
    }

    @Override
    public Object evaluateValues(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        } else if (allNullValues(values)) {
            return null;
        } else if (allFloats(values) || allDoubles(values)) {
            return sumOfDoubleValues(values);
        } else if (allBigDecimals(values)) {
            return sumOfBigDecimals(values);
        } else {
            return sumOfLongValues(values);
        }
    }



}
