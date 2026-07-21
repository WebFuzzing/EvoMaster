package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.conditions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deep-clones a {@link CypherCondition} (or {@link Operand}) while substituting the variable names it
 * refers to, according to a rename map. Used by {@link Neo4jPatternExpander} when it clones a
 * variable-length edge into a chain: the edge's type/property conditions and the endpoints' labels
 * must be re-bound to the fresh variable names of the cloned edges and intermediate nodes.
 * <p>
 * Only variable names change; operators, literals and structure are preserved. A name not present in
 * the map is left unchanged.
 */
final class ConditionRenamer {

    private ConditionRenamer() {
    }

    static CypherCondition rename(CypherCondition c, Map<String, String> renames) {
        if (c instanceof LabelCondition) {
            LabelCondition lc = (LabelCondition) c;
            return new LabelCondition(map(lc.getVariableName(), renames), lc.getLabel());
        }
        if (c instanceof AnyLabelCondition) {
            return new AnyLabelCondition(map(((AnyLabelCondition) c).getVariableName(), renames));
        }
        if (c instanceof TypeCondition) {
            TypeCondition tc = (TypeCondition) c;
            return new TypeCondition(map(tc.getVariableName(), renames), tc.getType());
        }
        if (c instanceof PropertyCondition) {
            PropertyCondition pc = (PropertyCondition) c;
            return new PropertyCondition(map(pc.getVariableName(), renames), pc.getPropertyKey(),
                    rename(pc.getValue(), renames));
        }
        if (c instanceof ComparisonCondition) {
            ComparisonCondition cc = (ComparisonCondition) c;
            return new ComparisonCondition(rename(cc.getLeft(), renames), cc.getOperator(),
                    rename(cc.getRight(), renames));
        }
        if (c instanceof AndCondition) {
            return new AndCondition(renameAll(((AndCondition) c).getConditions(), renames));
        }
        if (c instanceof OrCondition) {
            return new OrCondition(renameAll(((OrCondition) c).getConditions(), renames));
        }
        if (c instanceof XorCondition) {
            return new XorCondition(renameAll(((XorCondition) c).getConditions(), renames));
        }
        if (c instanceof NotCondition) {
            return new NotCondition(rename(((NotCondition) c).getCondition(), renames));
        }
        return c;
    }

    private static List<CypherCondition> renameAll(List<CypherCondition> conditions,
                                                   Map<String, String> renames) {
        List<CypherCondition> out = new ArrayList<>(conditions.size());
        for (CypherCondition c : conditions) {
            out.add(rename(c, renames));
        }
        return out;
    }

    static Operand rename(Operand o, Map<String, String> renames) {
        if (o instanceof PropertyOperand) {
            PropertyOperand po = (PropertyOperand) o;
            return new PropertyOperand(map(po.getVariableName(), renames), po.getPropertyKey());
        }
        if (o instanceof ArithmeticOperand) {
            ArithmeticOperand ao = (ArithmeticOperand) o;
            Operand right = ao.getRight() == null ? null : rename(ao.getRight(), renames);
            return new ArithmeticOperand(ao.getOperator(), rename(ao.getLeft(), renames), right);
        }
        if (o instanceof ListOperand) {
            List<Operand> elements = new ArrayList<>();
            for (Operand e : ((ListOperand) o).getElements()) {
                elements.add(rename(e, renames));
            }
            return new ListOperand(elements);
        }
        return o;
    }

    /** Returns the renamed name if present in the map, otherwise the original. */
    private static String map(String name, Map<String, String> renames) {
        String renamed = renames.get(name);
        return renamed != null ? renamed : name;
    }

    /**
     * True when the condition references {@code variable} as a node/edge variable. Used to decide
     * which endpoint conditions an intermediate node should inherit.
     */
    static boolean referencesVariable(CypherCondition c, String variable) {
        if (c instanceof LabelCondition) {
            return variable.equals(((LabelCondition) c).getVariableName());
        }
        if (c instanceof AnyLabelCondition) {
            return variable.equals(((AnyLabelCondition) c).getVariableName());
        }
        if (c instanceof PropertyCondition) {
            return variable.equals(((PropertyCondition) c).getVariableName());
        }
        return false;
    }
}
