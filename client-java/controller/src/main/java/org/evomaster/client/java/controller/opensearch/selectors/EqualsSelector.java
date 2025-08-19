package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.EqualsOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.selectors.SingleConditionQuerySelector;

/**
 * { field: { $eq: value } }
 */
public class EqualsSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new EqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "Term";
    }
}