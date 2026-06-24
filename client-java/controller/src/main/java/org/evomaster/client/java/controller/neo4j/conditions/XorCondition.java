package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a logical XOR of multiple conditions.
 */
public class XorCondition implements CypherCondition {

    private final List<CypherCondition> conditions;

    public XorCondition(List<CypherCondition> conditions) {
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
    }

    public List<CypherCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(" XOR ");
            sb.append(conditions.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XorCondition that = (XorCondition) o;
        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }
}
