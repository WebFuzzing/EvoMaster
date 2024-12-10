package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $or operation.
 * Selects the documents that satisfy at least one of the conditions.
 */
public class OrOperation extends QueryOperation{
    private final List<QueryOperation> conditions;

    public OrOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    public List<QueryOperation> getConditions() {
        return conditions;
    }
}