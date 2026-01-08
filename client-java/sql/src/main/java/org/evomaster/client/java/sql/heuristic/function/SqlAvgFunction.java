package org.evomaster.client.java.sql.heuristic.function;

import java.math.BigDecimal;
import java.util.List;

import static org.evomaster.client.java.sql.heuristic.function.SqlAggregateFunctionUtils.*;


public class SqlAvgFunction extends SqlAggregateFunction {

    private static final String AVG_FUNCTION_NAME = "AVG";

    public SqlAvgFunction() {
        super(AVG_FUNCTION_NAME);
    }

    @Override
    public Object evaluateValues(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        } else if (allNullValues(values)) {
            return null;
        } else if (allFloats(values) || allDoubles(values)) {
            double sum = sumOfDoubleValues(values);
            double avg = sum / countNonNullValues(values);
            return avg;
        } else if (allBigDecimals(values)) {
            BigDecimal sum = sumOfBigDecimals(values);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(countNonNullValues(values)));
            return avg;
        } else {
            long sum = sumOfLongValues(values);
            long avg = sum / countNonNullValues(values);
            return avg;
        }
    }


}
