package org.evomaster.client.java.controller.dynamodb.operations;

import java.util.List;

public class OrOperation extends QueryOperation {

    private final List<QueryOperation> conditions;

    public OrOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    public List<QueryOperation> getConditions() {
        return conditions;
    }
}
