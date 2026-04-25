package org.evomaster.client.java.controller.dynamodb.operations;

/**
 * DynamoDB existence predicate operation for {@code attribute_exists} and
 * {@code attribute_not_exists}.
 */
public class ExistsOperation extends QueryOperation {

    private final String fieldName;
    //true = exists, false = not exists
    private final boolean exists;

    /**
     * Creates an existence operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param exists {@code true} for exists, {@code false} for not-exists
     */
    public ExistsOperation(String fieldName, boolean exists) {
        this.fieldName = fieldName;
        this.exists = exists;
    }

    /**
     * @return field name coming from DynamoDB expression/condition
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return {@code true} when existence is required, {@code false} otherwise
     */
    public boolean isExists() {
        return exists;
    }
}
