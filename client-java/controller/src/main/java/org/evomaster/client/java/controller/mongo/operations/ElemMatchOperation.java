package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $elemMatch operation.
 * Selects documents if element in the array field matches all the specified $elemMatch conditions.
 * Here it only has one condition to match implementation in "com.mongodb.client.model.Filters"
 */
public class ElemMatchOperation extends QueryOperation{
    private final String fieldName;
    private final QueryOperation condition;

    public ElemMatchOperation(String fieldName, QueryOperation condition) {
        this.fieldName = fieldName;
        this.condition = condition;
    }

    public String getFieldName() {
        return fieldName;
    }

    public QueryOperation getCondition() {
        return condition;
    }
}