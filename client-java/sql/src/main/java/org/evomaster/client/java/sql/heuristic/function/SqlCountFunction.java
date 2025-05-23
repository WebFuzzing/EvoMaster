package org.evomaster.client.java.sql.heuristic.function;

import org.evomaster.client.java.sql.DataRow;

import java.util.List;

import static org.evomaster.client.java.sql.heuristic.function.SqlAggregateFunctionUtils.countNonNullValues;

public class SqlCountFunction extends SqlAggregateFunction {

    private static final String COUNT_FUNCTION_NAME = "COUNT";

    public SqlCountFunction() {
        super(COUNT_FUNCTION_NAME);
    }

    @Override
    public Object evaluateValues(List<Object> values) {
        final long count;
        if (values.isEmpty()) {
            count = 0;
        } else if (isListOfDataRows(values)) {
            /*
             * In SQL, COUNT(*) counts NULLs all rows,
             * while COUNT(column) counts only non-nulls
             */
            count = values.size();
        } else {
            count = countNonNullValues(values);
        }
        return count;
    }

    private static boolean isListOfDataRows(List<Object> values) {
        for (Object value : values) {
            if (!(value instanceof DataRow)) {
                return false;
            }
        }
        return true;
    }

}
