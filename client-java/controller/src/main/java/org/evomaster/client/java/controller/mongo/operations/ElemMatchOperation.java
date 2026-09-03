package org.evomaster.client.java.controller.mongo.operations;

import java.util.Objects;

/**
 * Represent $elemMatch operation.
 * Selects documents if element in the array field matches all the specified $elemMatch conditions.
 * Here it only has one condition to match implementation in "com.mongodb.client.model.Filters"
 */
public class ElemMatchOperation extends QueryOperationWithField {
    private final QueryOperation condition;

    public ElemMatchOperation(String fieldName, QueryOperation condition) {
        super(fieldName);
        Objects.requireNonNull(condition);
        this.condition = condition;
    }

    public QueryOperation getCondition() {
        return condition;
    }
}
