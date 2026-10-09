package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.List;
import java.util.Objects;

/**
 * Represents an operator applied to some arguments, like {$add: ["$a", "$b"]}.
 */
public class OperatorExpression extends AggregationExpression {

    private final ExpressionOperator operator;
    private final List<AggregationExpression> arguments;

    public OperatorExpression(ExpressionOperator operator, List<AggregationExpression> arguments) {
        this.operator = Objects.requireNonNull(operator);
        this.arguments = Objects.requireNonNull(arguments);
    }

    public ExpressionOperator getOperator() {
        return operator;
    }

    /**
     * @return the arguments of the operator. For $cond, they are always the condition, the value if it is
     * true, and the value if it is not (even if it was written as a document with if, then and else).
     */
    public List<AggregationExpression> getArguments() {
        return arguments;
    }
}
