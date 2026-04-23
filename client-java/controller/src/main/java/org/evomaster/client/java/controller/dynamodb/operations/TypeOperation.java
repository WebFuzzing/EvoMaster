package org.evomaster.client.java.controller.dynamodb.operations;

/**
 * DynamoDB {@code attribute_type(path, type)} predicate operation.
 */
public class TypeOperation extends QueryOperation {

    private final String fieldName;
    private final String expectedType;

    /**
     * Creates a type operation.
     *
     * @param fieldName attribute path
     * @param expectedType expected DynamoDB type token
     */
    public TypeOperation(String fieldName, String expectedType) {
        this.fieldName = fieldName;
        this.expectedType = expectedType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getExpectedType() {
        return expectedType;
    }
}
