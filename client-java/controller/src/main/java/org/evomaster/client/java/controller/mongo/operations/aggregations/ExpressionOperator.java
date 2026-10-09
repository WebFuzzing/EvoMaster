package org.evomaster.client.java.controller.mongo.operations.aggregations;

/**
 * The operators of the expressions that are supported.
 */
public enum ExpressionOperator {
    SUM("$sum"),
    ADD("$add"),
    SUBTRACT("$subtract"),
    MULTIPLY("$multiply"),
    DIVIDE("$divide"),
    CONCAT("$concat"),
    TO_LOWER("$toLower"),
    TO_UPPER("$toUpper"),
    SIZE("$size"),
    IF_NULL("$ifNull"),
    COND("$cond"),
    EQ("$eq"),
    NE("$ne"),
    GT("$gt"),
    GTE("$gte"),
    LT("$lt"),
    LTE("$lte");

    private final String operatorName;

    ExpressionOperator(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    /**
     * @return the operator with the given name (including the dollar sign), or null if there is none
     */
    public static ExpressionOperator fromName(String operatorName) {
        for (ExpressionOperator operator : values()) {
            if (operator.operatorName.equals(operatorName)) {
                return operator;
            }
        }
        return null;
    }
}
