package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents a field of a $project stage.
 */
public class ProjectField {

    private final String fieldPath;
    private final ProjectFieldType type;
    private final AggregationExpression expression;

    /**
     * @param fieldPath  the dot-separated path of the field
     * @param type       whether the field is included, excluded or computed
     * @param expression the expression that computes the field. It is only for computed fields, otherwise null.
     */
    public ProjectField(String fieldPath, ProjectFieldType type, AggregationExpression expression) {
        this.fieldPath = Objects.requireNonNull(fieldPath);
        this.type = Objects.requireNonNull(type);
        this.expression = expression;
        if ((type == ProjectFieldType.COMPUTED) != (expression != null)) {
            throw new IllegalArgumentException("Only computed fields have an expression: " + fieldPath);
        }
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public ProjectFieldType getType() {
        return type;
    }

    public AggregationExpression getExpression() {
        return expression;
    }
}
