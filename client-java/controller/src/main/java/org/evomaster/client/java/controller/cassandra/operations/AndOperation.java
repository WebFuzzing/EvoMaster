package org.evomaster.client.java.controller.cassandra.operations;

import java.util.List;

/**
 * Represents a CQL AND operation.
 * Joins query clauses with the logical AND returns all rows that match the conditions of all clauses.
 */
public class AndOperation extends CqlQueryOperation {
    private final List<CqlQueryOperation> conditions;

    public AndOperation(List<CqlQueryOperation> conditions) {
        this.conditions = conditions;
    }

    public List<CqlQueryOperation> getConditions() {
        return conditions;
    }
}
