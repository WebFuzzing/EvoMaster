package org.evomaster.client.java.controller.dynamodb.operations;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeType;

/**
 * DynamoDB {@code attribute_type(path, type)} predicate operation.
 */
public class TypeOperation extends QueryOperation {

    private final String fieldName;
    private final DynamoDbAttributeType expectedType;

    /**
     * Creates a type operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param expectedType expected DynamoDB attribute type
     */
    public TypeOperation(String fieldName, DynamoDbAttributeType expectedType) {
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
     * @return expected DynamoDB attribute type
     */
    public DynamoDbAttributeType getExpectedType() {
        return expectedType;
    }
}
