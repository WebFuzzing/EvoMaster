package org.evomaster.client.java.controller.mongo.operations.aggregations;

/**
 * The accumulators of a $group stage that are supported.
 */
public enum AccumulatorOperator {
    SUM("$sum"),
    AVG("$avg"),
    MIN("$min"),
    MAX("$max"),
    FIRST("$first"),
    LAST("$last"),
    PUSH("$push"),
    ADD_TO_SET("$addToSet"),
    COUNT("$count");

    private final String operatorName;

    AccumulatorOperator(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    /**
     * @return the operator with the given name (including the dollar sign), or null if there is none
     */
    public static AccumulatorOperator fromName(String operatorName) {
        for (AccumulatorOperator operator : values()) {
            if (operator.operatorName.equals(operatorName)) {
                return operator;
            }
        }
        return null;
    }
}
