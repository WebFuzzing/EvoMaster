package org.evomaster.client.java.controller.dynamodb.operations;

/**
 * DynamoDB {@code contains(path, value)} predicate operation.
 */
public class ContainsOperation extends QueryOperation {

    private final String fieldName;
    private final Object expectedValue;

    /**
     * Creates a contains operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param expectedValue expected contained value
     */
    public ContainsOperation(String fieldName, Object expectedValue) {
        this.fieldName = fieldName;
        this.expectedValue = expectedValue;
    }

    /**
     * @return field name coming from DynamoDB expression/condition
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return expected contained value
     */
    public Object getExpectedValue() {
        return expectedValue;
    }
}
