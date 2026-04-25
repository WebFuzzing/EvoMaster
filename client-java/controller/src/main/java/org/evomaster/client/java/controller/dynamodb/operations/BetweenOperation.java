package org.evomaster.client.java.controller.dynamodb.operations;

/**
 * DynamoDB {@code path BETWEEN lower AND upper} predicate operation.
 */
public class BetweenOperation extends QueryOperation {

    private final String fieldName;
    private final Object lowerBound;
    private final Object upperBound;

    /**
     * Creates a BETWEEN operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param lowerBound lower bound value
     * @param upperBound upper bound value
     */
    public BetweenOperation(String fieldName, Object lowerBound, Object upperBound) {
        this.fieldName = fieldName;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * @return field name coming from DynamoDB expression/condition
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return lower bound value
     */
    public Object getLowerBound() {
        return lowerBound;
    }

    /**
     * @return upper bound value
     */
    public Object getUpperBound() {
        return upperBound;
    }
}
