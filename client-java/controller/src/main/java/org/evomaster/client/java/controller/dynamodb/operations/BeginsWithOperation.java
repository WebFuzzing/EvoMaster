package org.evomaster.client.java.controller.dynamodb.operations;

/**
 * DynamoDB {@code begins_with(path, value)} predicate operation.
 */
public class BeginsWithOperation extends QueryOperation {

    private final String fieldName;
    private final Object prefix;

    /**
     * Creates a begins-with operation.
     *
     * @param fieldName attribute path
     * @param prefix expected prefix
     */
    public BeginsWithOperation(String fieldName, Object prefix) {
        this.fieldName = fieldName;
        this.prefix = prefix;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getPrefix() {
        return prefix;
    }
}
