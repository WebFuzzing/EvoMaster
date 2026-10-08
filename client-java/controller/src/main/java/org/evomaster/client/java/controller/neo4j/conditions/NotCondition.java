package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * Represents a logical NOT of a condition.
 */
public class NotCondition implements CypherCondition {

    private final CypherCondition condition;

    public NotCondition(CypherCondition condition) {
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
    }

    public CypherCondition getCondition() {
        return condition;
    }

    @Override
    public <T> T accept(CypherConditionVisitor<T> visitor) {
        return visitor.visitNot(this);
    }

    @Override
    public String toString() {
        return "NOT " + condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotCondition that = (NotCondition) o;
        return Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return condition != null ? condition.hashCode() : 0;
    }
}
