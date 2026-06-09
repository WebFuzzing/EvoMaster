package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a logical AND of multiple conditions.
 */
public class AndCondition implements CypherCondition {

    private final List<CypherCondition> conditions;

    public AndCondition(List<CypherCondition> conditions) {
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
    }

    public List<CypherCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(conditions.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AndCondition that = (AndCondition) o;
        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }
}
