package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.conditions.*;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.controller.neo4j.data.Neo4jRelationship;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator.C;
import static org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator.FALSE_TRUTHNESS;
import static org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator.TRUE_TRUTHNESS;

/**
 * Evaluates the truthness {@code ρ(condition, m)} of a single {@link CypherCondition} under a
 * structural mapping {@code m}, recursively over the typed boolean tree the parser produces
 * (And/Or/Xor/Not, comparisons, label/type/property leaves).
 * <p>
 * Returns {@code null} when a condition cannot be valuated under the mapping (e.g. an absent
 * property, an unbound variable, or an opaque/raw operand); the caller's aggregation skips
 * {@code null} results.
 */
class Neo4jConditionEvaluator {

    private static final Object UNRESOLVED = new Object();

    private final TaintHandler taintHandler;

    Neo4jConditionEvaluator() {
        this(null);
    }

    Neo4jConditionEvaluator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    /**
     * Returns {@code ρ(condition, mapping)}, or {@code null} when the condition cannot be valuated
     * and must be skipped by the aggregation.
     */
    Truthness rho(CypherCondition condition, Neo4jMapping mapping) {
        if (condition instanceof LabelCondition) {
            LabelCondition lc = (LabelCondition) condition;
            Neo4jNode node = mapping.getNode(lc.getVariableName());
            return node == null ? null : labelInSet(lc.getLabel(), node.getLabels());
        }
        if (condition instanceof AnyLabelCondition) {
            Neo4jNode node = mapping.getNode(((AnyLabelCondition) condition).getVariableName());
            if (node == null) {
                return null;
            }
            return node.getLabels().isEmpty() ? FALSE_TRUTHNESS : TRUE_TRUTHNESS;
        }
        if (condition instanceof TypeCondition) {
            TypeCondition tc = (TypeCondition) condition;
            Neo4jRelationship rel = mapping.getEdge(tc.getVariableName());
            if (rel == null) {
                return null;
            }
            taintStringEquals(rel.getType(), tc.getType());
            return stringEqualityTruthness(rel.getType(), tc.getType());
        }
        if (condition instanceof PropertyCondition) {
            return rhoProperty((PropertyCondition) condition, mapping);
        }
        if (condition instanceof ComparisonCondition) {
            return rhoComparison((ComparisonCondition) condition, mapping);
        }
        if (condition instanceof AndCondition) {
            return aggregate(((AndCondition) condition).getConditions(), mapping, true);
        }
        if (condition instanceof OrCondition) {
            return aggregate(((OrCondition) condition).getConditions(), mapping, false);
        }
        if (condition instanceof XorCondition) {
            return rhoXor(((XorCondition) condition).getConditions(), mapping);
        }
        if (condition instanceof NotCondition) {
            Truthness inner = rho(((NotCondition) condition).getCondition(), mapping);
            return inner == null ? null : inner.invert();
        }
        return null;
    }

    private Truthness rhoProperty(PropertyCondition pc, Neo4jMapping mapping) {
        Object actual = resolveProperty(pc.getVariableName(), pc.getPropertyKey(), mapping);
        if (actual == UNRESOLVED) {
            return null;
        }
        Object expected = valuate(pc.getValue(), mapping);
        if (expected == UNRESOLVED) {
            return null;
        }
        return equalityTruthness(actual, expected);
    }

    private Truthness rhoComparison(ComparisonCondition cc, Neo4jMapping mapping) {
        switch (cc.getOperator()) {
            case IS_NULL:
                return presenceTruthness(cc.getLeft(), mapping, /*wantPresent=*/false);
            case IS_NOT_NULL:
                return presenceTruthness(cc.getLeft(), mapping, /*wantPresent=*/true);
            case IN:
                return rhoIn(cc, mapping);
            case STARTS_WITH:
            case ENDS_WITH:
            case CONTAINS:
                return rhoStringPredicate(cc, mapping);
            default:
                return rhoBinary(cc, mapping);
        }
    }

    private Truthness rhoBinary(ComparisonCondition cc, Neo4jMapping mapping) {
        Object l = valuate(cc.getLeft(), mapping);
        Object r = valuate(cc.getRight(), mapping);
        if (l == UNRESOLVED || r == UNRESOLVED || l == null || r == null) {
            return null;
        }
        switch (cc.getOperator()) {
            case EQUALS:
                return equalityTruthness(l, r);
            case NOT_EQUALS:
                return equalityTruthness(l, r).invert();
            case LESS_THAN:
                return numericLessThan(l, r);
            case GREATER_THAN:
                return numericLessThan(r, l);
            case LESS_THAN_OR_EQUALS: {
                Truthness t = numericLessThan(r, l);
                return t == null ? null : t.invert();
            }
            case GREATER_THAN_OR_EQUALS: {
                Truthness t = numericLessThan(l, r);
                return t == null ? null : t.invert();
            }
            default:
                return null;
        }
    }

    private Truthness rhoIn(ComparisonCondition cc, Neo4jMapping mapping) {
        Object l = valuate(cc.getLeft(), mapping);
        if (l == UNRESOLVED || l == null || !(cc.getRight() instanceof ListOperand)) {
            return null;
        }
        List<Operand> elements = ((ListOperand) cc.getRight()).getElements();
        List<Truthness> truths = new ArrayList<>();
        for (Operand element : elements) {
            Object ev = valuate(element, mapping);
            if (ev == UNRESOLVED || ev == null) {
                continue;
            }
            truths.add(equalityTruthness(l, ev));
        }
        if (truths.isEmpty()) {
            return null;
        }
        return TruthnessUtils.buildOrAggregationTruthness(truths.toArray(new Truthness[0]));
    }

    private Truthness rhoStringPredicate(ComparisonCondition cc, Neo4jMapping mapping) {
        Object l = valuate(cc.getLeft(), mapping);
        Object r = valuate(cc.getRight(), mapping);
        if (!(l instanceof String) || !(r instanceof String)) {
            return null;
        }
        switch (cc.getOperator()) {
            case STARTS_WITH:
                return getStartsWith((String) l, (String) r);
            case ENDS_WITH:
                return getEndsWith((String) l, (String) r);
            case CONTAINS:
                return getContains((String) l, (String) r);
            default:
                return null;
        }
    }

    private Truthness rhoXor(List<CypherCondition> conditions, Neo4jMapping mapping) {
        Truthness acc = null;
        for (CypherCondition c : conditions) {
            Truthness t = rho(c, mapping);
            if (t == null) {
                continue;
            }
            acc = (acc == null) ? t : TruthnessUtils.buildXorAggregationTruthness(acc, t);
        }
        return acc;
    }

    /** AND/OR aggregation over a child list, skipping children that cannot be valuated. */
    private Truthness aggregate(List<CypherCondition> conditions, Neo4jMapping mapping, boolean and) {
        List<Truthness> truths = new ArrayList<>();
        for (CypherCondition c : conditions) {
            Truthness t = rho(c, mapping);
            if (t != null) {
                truths.add(t);
            }
        }
        if (truths.isEmpty()) {
            return null;
        }
        Truthness[] arr = truths.toArray(new Truthness[0]);
        return and ? TruthnessUtils.buildAndAggregationTruthness(arr)
                : TruthnessUtils.buildOrAggregationTruthness(arr);
    }

    /**
     * Valuates an operand under the mapping. Returns {@link #UNRESOLVED} for an absent property,
     * an opaque {@link RawOperand}, a list (handled only inside IN), an unbound variable, or an
     * arithmetic expression over a non-numeric / unresolved side.
     */
    private Object valuate(Operand operand, Neo4jMapping mapping) {
        if (operand instanceof LiteralOperand) {
            return ((LiteralOperand) operand).getValue();
        }
        if (operand instanceof PropertyOperand) {
            PropertyOperand po = (PropertyOperand) operand;
            return resolveProperty(po.getVariableName(), po.getPropertyKey(), mapping);
        }
        if (operand instanceof ArithmeticOperand) {
            return valuateArithmetic((ArithmeticOperand) operand, mapping);
        }
        return UNRESOLVED;
    }

    private Object valuateArithmetic(ArithmeticOperand ao, Neo4jMapping mapping) {
        if (ao.getOperator() == ArithmeticOperator.NEGATE) {
            Object v = valuate(ao.getLeft(), mapping);
            Double d = asDouble(v);
            return d == null ? UNRESOLVED : -d;
        }
        Double l = asDouble(valuate(ao.getLeft(), mapping));
        Double r = asDouble(valuate(ao.getRight(), mapping));
        if (l == null || r == null) {
            return UNRESOLVED;
        }
        switch (ao.getOperator()) {
            case PLUS: return l + r;
            case MINUS: return l - r;
            case TIMES: return l * r;
            case DIVIDE: return r == 0d ? UNRESOLVED : l / r;
            case MODULO: return r == 0d ? UNRESOLVED : l % r;
            case POWER: return Math.pow(l, r);
            default: return UNRESOLVED;
        }
    }

    /** Resolves {@code variable.key} from the node or relationship the variable is bound to. */
    private Object resolveProperty(String variable, String key, Neo4jMapping mapping) {
        Neo4jNode node = mapping.getNode(variable);
        if (node != null) {
            return node.hasProperty(key) ? node.getProperty(key) : UNRESOLVED;
        }
        Neo4jRelationship rel = mapping.getEdge(variable);
        if (rel != null) {
            return rel.hasProperty(key) ? rel.getProperty(key) : UNRESOLVED;
        }
        return UNRESOLVED;
    }

    /** ρ for IS NULL / IS NOT NULL: a presence check with no gradient. */
    private Truthness presenceTruthness(Operand operand, Neo4jMapping mapping, boolean wantPresent) {
        if (!(operand instanceof PropertyOperand)) {
            return null;
        }
        PropertyOperand po = (PropertyOperand) operand;
        Object value = resolveProperty(po.getVariableName(), po.getPropertyKey(), mapping);
        boolean present = value != UNRESOLVED && value != null;
        boolean satisfied = wantPresent == present;
        return satisfied ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
    }

    private Truthness equalityTruthness(Object a, Object b) {
        if (a == null || b == null) {
            return null;
        }
        Double da = asDouble(a);
        Double db = asDouble(b);
        if (da != null && db != null) {
            return TruthnessUtils.getEqualityTruthness((double) da, (double) db);
        }
        if (a instanceof String && b instanceof String) {
            taintStringEquals((String) a, (String) b);
            return stringEqualityTruthness((String) a, (String) b);
        }
        return a.equals(b) ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
    }

    /** Feeds a string equality to the taint handler (no-op if there is no handler or no tainted input). */
    private void taintStringEquals(String a, String b) {
        if (taintHandler != null && a != null && b != null) {
            taintHandler.handleTaintForStringEquals(a, b, false);
        }
    }

    private Truthness numericLessThan(Object a, Object b) {
        Double da = asDouble(a);
        Double db = asDouble(b);
        if (da == null || db == null) {
            return null;
        }
        return TruthnessUtils.getLessThanTruthness((double) da, (double) db);
    }

    /**
     * Direct string equality truthness: {@code TRUE} when the strings are equal, otherwise {@code ofTrue}
     * is the left-alignment edit distance scaled from base {@code C} (so it never drops below {@code C})
     * and {@code ofFalse = 1}. Used where a string is compared for equality on its own — relationship
     * types and string-valued property/WHERE equality.
     */
    static Truthness stringEqualityTruthness(String a, String b) {
        if (a == null || b == null) {
            return null;
        }
        if (a.equals(b)) {
            return TRUE_TRUTHNESS;
        }
        double distance = DistanceHelper.getLeftAlignmentDistance(a, b);
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(C, distance);
        return new Truthness(h, 1d);
    }

    /**
     * Un-based string similarity ({@code ofTrue = 1 - normalize(distance)}), used only as the per-label
     * score inside {@link #labelInSet}, which then re-bases the best of them with {@code scaleTrue(C, ...)}.
     * Kept separate from {@link #stringEqualityTruthness} so the base {@code C} is applied exactly once
     * on the label path (applying it here as well would double-count it).
     */
    static Truthness stringSimilarityTruthness(String a, String b) {
        if (a == null || b == null) {
            return null;
        }
        long distance = DistanceHelper.getLeftAlignmentDistance(a, b);
        double ofTrue = 1d - TruthnessUtils.normalizeValue((double) distance);
        return new Truthness(ofTrue, a.equals(b) ? 0d : 1d);
    }

    /**
     * {@code label_in_set(L, labels)}: TRUE if the exact label is present; FALSE if the element has
     * no labels; otherwise the best per-label string similarity scaled from base {@code C}.
     */
    static Truthness labelInSet(String label, Set<String> labels) {
        if (labels.contains(label)) {
            return TRUE_TRUTHNESS;
        }
        if (labels.isEmpty()) {
            return FALSE_TRUTHNESS;
        }
        double maxOfTrue = 0d;
        for (String present : labels) {
            Truthness t = stringSimilarityTruthness(label, present);
            if (t.getOfTrue() > maxOfTrue) {
                maxOfTrue = t.getOfTrue();
            }
        }
        return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    static Truthness getStartsWith(String str, String prefix) {
        if (str.startsWith(prefix)) {
            return TRUE_TRUTHNESS;
        }
        int n = Math.min(str.length(), prefix.length());
        String actualPrefix = str.substring(0, n);
        double distance = DistanceHelper.getLeftAlignmentDistance(actualPrefix, prefix);
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(C, distance);
        return new Truthness(h, 1d);
    }

    static Truthness getEndsWith(String str, String suffix) {
        if (str.endsWith(suffix)) {
            return TRUE_TRUTHNESS;
        }
        int n = Math.min(str.length(), suffix.length());
        String actualSuffix = str.substring(str.length() - n);
        String expectedSuffix = suffix.substring(suffix.length() - n);
        double distance = DistanceHelper.getLeftAlignmentDistance(actualSuffix, expectedSuffix);
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(C, distance);
        return new Truthness(h, 1d);
    }

    static Truthness getContains(String str, String substring) {
        if (str.contains(substring)) {
            return TRUE_TRUTHNESS;
        }
        double distance = getMinSubstringDistance(str, substring);
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(C, distance);
        return new Truthness(h, 1d);
    }

    private static double getMinSubstringDistance(String str, String substring) {
        if (str.length() < substring.length()) {
            return DistanceHelper.getLeftAlignmentDistance(str, substring);
        }
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i <= str.length() - substring.length(); i++) {
            String window = str.substring(i, i + substring.length());
            double distance = DistanceHelper.getLeftAlignmentDistance(window, substring);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }
}
