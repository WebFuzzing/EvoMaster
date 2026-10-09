package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents a field of a $group stage, computed for each group with an accumulator
 * (like an aggregate function in SQL).
 */
public class GroupAccumulator {

    private final String fieldName;
    private final String operatorName;
    private final AccumulatorOperator operator;
    private final AggregationExpression argument;

    /**
     * @param fieldName    the name of the field in the result
     * @param operatorName the operator as written in the pipeline, including the dollar sign (eg, $sum)
     * @param operator     the operator, or null if it is not supported
     * @param argument     the expression that the accumulator is computed on. It is null if the operator does not
     *                     have one ($count).
     */
    public GroupAccumulator(String fieldName, String operatorName, AccumulatorOperator operator, AggregationExpression argument) {
        this.fieldName = Objects.requireNonNull(fieldName);
        this.operatorName = Objects.requireNonNull(operatorName);
        this.operator = operator;
        this.argument = argument;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    /**
     * @return the operator, or null if it is not supported
     */
    public AccumulatorOperator getOperator() {
        return operator;
    }

    public boolean isSupported() {
        return operator != null;
    }

    /**
     * @return the expression the accumulator is computed on, or null for $count
     */
    public AggregationExpression getArgument() {
        return argument;
    }
}
