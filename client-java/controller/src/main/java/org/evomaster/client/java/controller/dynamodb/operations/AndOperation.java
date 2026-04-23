package org.evomaster.client.java.controller.dynamodb.operations;

import java.util.List;

public class AndOperation extends QueryOperation {

    private final List<QueryOperation> conditions;

    public AndOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    public List<QueryOperation> getConditions() {
        return conditions;
    }
}
