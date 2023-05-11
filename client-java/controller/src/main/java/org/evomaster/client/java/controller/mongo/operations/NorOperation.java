package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $nor operation.
 * Selects the documents that fail all the query expressions in the array.
 */
public class NorOperation extends QueryOperation{
    private final List<QueryOperation> conditions;

    public NorOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    public List<QueryOperation> getConditions() {
        return conditions;
    }
}