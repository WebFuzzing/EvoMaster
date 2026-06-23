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
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param expectedType expected DynamoDB type token
     */
    public TypeOperation(String fieldName, String expectedType) {
        this.fieldName = fieldName;
        this.expectedType = expectedType;
    }

    /**
     * @return field name coming from DynamoDB expression/condition
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return expected DynamoDB type token
     */
    public String getExpectedType() {
        return expectedType;
    }
}
