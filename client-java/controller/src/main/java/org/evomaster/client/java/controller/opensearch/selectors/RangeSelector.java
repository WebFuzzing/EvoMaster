package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.RangeOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

/**
 * { range: { field: range_logic } }
 */
public class RangeSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new RangeOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "Range";
    }

    @Override
    protected String structure() {
        return "range";
    }
}
