package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $and operation.
 * Joins query clauses with a logical AND returns all documents that match the conditions of all clauses.
 */
public class AndOperation extends QueryOperation{
    private final List<QueryOperation> conditions;

    public AndOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    public List<QueryOperation> getConditions() {
        return conditions;
    }
}