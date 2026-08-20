package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.conditions.*;
import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator.C;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.FALSE_TRUTHNESS;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.TRUE_TRUTHNESS;

/**
 * Evaluates the truthness {@code ρ(condition, m)} of a single {@link CypherCondition} under a
 * structural mapping {@code m}, recursively over the typed boolean tree the parser produces
 * (And/Or/Xor/Not, comparisons, label/type/property leaves).
 * <p>
 * A condition that cannot be valuated under the mapping (an absent property, an unbound variable, an
 * opaque/raw operand) scores {@link #UNVALUATABLE} rather than {@code null}: a low but non-zero
 * {@code ofTrue} that still takes part in the aggregation. Never returns {@code null}.
 */
public class Neo4jConditionEvaluator {

    /**
     * Marker returned by {@link #resolveOperandValue} when an operand has no value under the current
     * mapping: an absent property, an unbound variable, or an operand the model does not decompose.
     * <p>
     * A distinct object rather than {@code null}, because {@code null} is itself a legitimate property
     * value in Cypher, and the two lead to different scores.
     */
    private static final Object UNRESOLVED = new Object();

    /**
     * Score of a condition that cannot be valuated under the mapping. It is aggregated like any other
     * score, so such a condition lowers the result without hiding the gradient of its siblings.
     * <p>
     * Compared by identity, so that NOT can propagate it instead of inverting its low {@code ofTrue}
     * into a near-true score.
     */
    public static final Truthness UNVALUATABLE = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);

    /** True when the given truthness is the {@link #UNVALUATABLE} marker, by identity. */
    private static boolean isUnvaluatable(Truthness t) {
        return t == UNVALUATABLE;
    }

    private final TaintHandler taintHandler;

    public Neo4jConditionEvaluator() {
        this(null);
    }

    public Neo4jConditionEvaluator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    /**
     * Returns {@code ρ(condition, mapping)}, never {@code null}: a condition that cannot be valuated
     * (an absent property, an unbound variable, an opaque {@link RawCondition}) scores
     * {@link #UNVALUATABLE}. Dispatch is done with a {@link CypherConditionVisitor} so every condition
     * type is handled explicitly: there is no {@code instanceof} chain and no silent fall-through for
     * an unhandled type.
     */
    public Truthness evaluateCondition(CypherCondition condition, Neo4jMapping mapping) {
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");
        return condition.accept(new TruthnessVisitor(mapping));
    }

    /**
     * Computes {@code ρ} for one condition under a fixed mapping. A fresh instance is created per
     * top-level condition (it carries the mapping), and it recurses through {@link #evaluateCondition}
     * for the boolean-tree children, so nested conditions dispatch through the same visitor.
     */
    public final class TruthnessVisitor implements CypherConditionVisitor<Truthness> {

        private final Neo4jMapping mapping;

        public TruthnessVisitor(Neo4jMapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public Truthness visitLabel(LabelCondition lc) {
            Neo4jNode node = mapping.getNode(lc.getVariableName());
            return node == null ? UNVALUATABLE : labelInSet(lc.getLabel(), node.getLabels());
        }

        @Override
        public Truthness visitAnyLabel(AnyLabelCondition ac) {
            Neo4jNode node = mapping.getNode(ac.getVariableName());
            if (node == null) {
                return UNVALUATABLE;
            }
            return node.getLabels().isEmpty() ? FALSE_TRUTHNESS : TRUE_TRUTHNESS;
        }

        @Override
        public Truthness visitType(TypeCondition tc) {
            Neo4jEdge rel = mapping.getEdge(tc.getVariableName());
            if (rel == null) {
                return UNVALUATABLE;
            }
            taintStringEquals(rel.getType(), tc.getType());
            return stringEqualityTruthness(rel.getType(), tc.getType());
        }

        @Override
        public Truthness visitProperty(PropertyCondition pc) {
            return evaluateProperty(pc, mapping);
        }

        @Override
        public Truthness visitComparison(ComparisonCondition cc) {
            return evaluateComparison(cc, mapping);
        }

        @Override
        public Truthness visitAnd(AndCondition ac) {
            return aggregate(ac.getConditions(), mapping, true);
        }

        @Override
        public Truthness visitOr(OrCondition oc) {
            return aggregate(oc.getConditions(), mapping, false);
        }

        @Override
        public Truthness visitXor(XorCondition xc) {
            return evaluateXor(xc.getConditions(), mapping);
        }

        @Override
        public Truthness visitNot(NotCondition nc) {
            Truthness inner = evaluateCondition(nc.getCondition(), mapping);
            // Cypher's NOT null is null, not true, so negating "no information" keeps it.
            return isUnvaluatable(inner) ? UNVALUATABLE : inner.invert();
        }

        @Override
        public Truthness visitRaw(RawCondition rc) {
            // A RawCondition is a WHERE predicate the parser kept as raw text because it could not
            // break it into operands. With no resolved operands there is no value to measure a distance
            // against.
            return UNVALUATABLE;
        }
    }

    private Truthness evaluateProperty(PropertyCondition pc, Neo4jMapping mapping) {
        Object actual = resolveProperty(pc.getVariableName(), pc.getPropertyKey(), mapping);
        if (actual == UNRESOLVED) {
            return UNVALUATABLE;
        }
        Object expected = resolveOperandValue(pc.getValue(), mapping);
        if (expected == UNRESOLVED) {
            return UNVALUATABLE;
        }
        return equalityTruthness(actual, expected);
    }

    private Truthness evaluateComparison(ComparisonCondition cc, Neo4jMapping mapping) {
        switch (cc.getOperator()) {
            case IS_NULL:
                return presenceTruthness(cc.getLeft(), mapping, /*wantPresent=*/false);
            case IS_NOT_NULL:
                return presenceTruthness(cc.getLeft(), mapping, /*wantPresent=*/true);
            case IN:
                return evaluateIn(cc, mapping);
            case STARTS_WITH:
            case ENDS_WITH:
            case CONTAINS:
                return evaluateStringPredicate(cc, mapping);
            case EQUALS:
            case NOT_EQUALS:
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_THAN_OR_EQUALS:
            case GREATER_THAN_OR_EQUALS:
                return evaluateBinaryComparison(cc, mapping);
            default:
                throw new IllegalArgumentException("Not supported operator: " + cc.getOperator());
        }
    }

    private Truthness evaluateBinaryComparison(ComparisonCondition cc, Neo4jMapping mapping) {
        Object l = resolveOperandValue(cc.getLeft(), mapping);
        Object r = resolveOperandValue(cc.getRight(), mapping);
        if (l == UNRESOLVED || r == UNRESOLVED || l == null || r == null) {
            return UNVALUATABLE;
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
                return isUnvaluatable(t) ? UNVALUATABLE : t.invert();
            }
            case GREATER_THAN_OR_EQUALS: {
                Truthness t = numericLessThan(l, r);
                return isUnvaluatable(t) ? UNVALUATABLE : t.invert();
            }
            default:
                throw new IllegalArgumentException("Not supported operator: " + cc.getOperator());
        }
    }

    private Truthness evaluateIn(ComparisonCondition cc, Neo4jMapping mapping) {
        Object l = resolveOperandValue(cc.getLeft(), mapping);
        if (l == UNRESOLVED || l == null || !(cc.getRight() instanceof ListOperand)) {
            return UNVALUATABLE;
        }
        List<Operand> elements = ((ListOperand) cc.getRight()).getElements();
        List<Truthness> listOfTruthness = new ArrayList<>();
        for (Operand element : elements) {
            Object ev = resolveOperandValue(element, mapping);
            if (ev == UNRESOLVED || ev == null) {
                continue;
            }
            listOfTruthness.add(equalityTruthness(l, ev));
        }
        if (listOfTruthness.isEmpty()) {
            return UNVALUATABLE;
        }
        return TruthnessUtils.buildOrAggregationTruthness(listOfTruthness.toArray(new Truthness[0]));
    }

    private Truthness evaluateStringPredicate(ComparisonCondition cc, Neo4jMapping mapping) {
        Object l = resolveOperandValue(cc.getLeft(), mapping);
        Object r = resolveOperandValue(cc.getRight(), mapping);
        if (!(l instanceof String) || !(r instanceof String)) {
            return UNVALUATABLE;
        }
        switch (cc.getOperator()) {
            case STARTS_WITH:
                return getStartsWith((String) l, (String) r);
            case ENDS_WITH:
                return getEndsWith((String) l, (String) r);
            case CONTAINS:
                return getContains((String) l, (String) r);
            default:
                throw new IllegalArgumentException("Not supported operator: " + cc.getOperator());
        }
    }

    /** XOR-folds the children, with the same handling of unvaluatable ones as {@link #aggregate}. */
    private Truthness evaluateXor(List<CypherCondition> conditions, Neo4jMapping mapping) {
        Truthness acc = null;
        boolean allUnvaluatable = true;
        for (CypherCondition c : conditions) {
            Truthness t = evaluateCondition(c, mapping);
            allUnvaluatable &= isUnvaluatable(t);
            acc = (acc == null) ? t : TruthnessUtils.buildXorAggregationTruthness(acc, t);
        }
        if (acc == null) {
            return UNVALUATABLE;
        }
        return allUnvaluatable ? UNVALUATABLE : acc;
    }

    /**
     * AND/OR aggregation over a child list. Every child takes part, so an AND is no longer reported as
     * satisfied just because its only valuatable branch holds. When they are all unvaluatable there is
     * nothing to measure, and the result is {@link #UNVALUATABLE} itself so the marker survives.
     */
    private Truthness aggregate(List<CypherCondition> conditions, Neo4jMapping mapping, boolean and) {
        List<Truthness> listOfTruthness = new ArrayList<>();
        boolean allUnvaluatable = true;
        for (CypherCondition c : conditions) {
            Truthness t = evaluateCondition(c, mapping);
            allUnvaluatable &= isUnvaluatable(t);
            listOfTruthness.add(t);
        }
        if (listOfTruthness.isEmpty() || allUnvaluatable) {
            return UNVALUATABLE;
        }
        Truthness[] arr = listOfTruthness.toArray(new Truthness[0]);
        return and ? TruthnessUtils.buildAndAggregationTruthness(arr)
                : TruthnessUtils.buildOrAggregationTruthness(arr);
    }

    /**
     * Resolves an operand's value ({@code v(x)} in {@code Formalizing.md}) under the mapping. Returns
     * {@link #UNRESOLVED} for an absent property, an opaque {@link RawOperand}, a list (handled only
     * inside IN), an unbound variable, or an arithmetic expression over a non-numeric / unresolved side.
     */
    private Object resolveOperandValue(Operand operand, Neo4jMapping mapping) {
        if (operand instanceof LiteralOperand) {
            return ((LiteralOperand) operand).getValue();
        }
        if (operand instanceof PropertyOperand) {
            PropertyOperand po = (PropertyOperand) operand;
            return resolveProperty(po.getVariableName(), po.getPropertyKey(), mapping);
        }
        if (operand instanceof ArithmeticOperand) {
            return resolveArithmeticOperandValue((ArithmeticOperand) operand, mapping);
        }
        return UNRESOLVED;
    }

    private Object resolveArithmeticOperandValue(ArithmeticOperand ao, Neo4jMapping mapping) {
        if (ao.getOperator() == ArithmeticOperator.NEGATE) {
            Object v = resolveOperandValue(ao.getLeft(), mapping);
            Double d = asDouble(v);
            return d == null ? UNRESOLVED : -d;
        }
        Double l = asDouble(resolveOperandValue(ao.getLeft(), mapping));
        Double r = asDouble(resolveOperandValue(ao.getRight(), mapping));
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
            default:
                throw new IllegalArgumentException("Not supported operator: " + ao.getOperator());
        }
    }

    /** Resolves {@code variable.key} from the node or relationship the variable is bound to. */
    private Object resolveProperty(String variable, String key, Neo4jMapping mapping) {
        Neo4jNode node = mapping.getNode(variable);
        if (node != null) {
            return node.hasProperty(key) ? node.getProperty(key) : UNRESOLVED;
        }
        Neo4jEdge rel = mapping.getEdge(variable);
        if (rel != null) {
            return rel.hasProperty(key) ? rel.getProperty(key) : UNRESOLVED;
        }
        return UNRESOLVED;
    }

    /** ρ for IS NULL / IS NOT NULL: a presence check with no gradient. */
    private Truthness presenceTruthness(Operand operand, Neo4jMapping mapping, boolean wantPresent) {
        if (!(operand instanceof PropertyOperand)) {
            return UNVALUATABLE;
        }
        PropertyOperand po = (PropertyOperand) operand;
        Object value = resolveProperty(po.getVariableName(), po.getPropertyKey(), mapping);
        boolean present = value != UNRESOLVED && value != null;
        boolean satisfied = wantPresent == present;
        return satisfied ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
    }

    /**
     * ρ for an equality {@code a = b} where either side is an already-valuated operand. The
     * right-hand value may be the Cypher {@code null} literal (a {@link LiteralOperand} whose value
     * is {@code null}); a {@code null} on either side scores {@link #UNVALUATABLE}, mirroring Cypher's
     * ternary logic where any comparison against {@code null} is {@code null} (unknown) rather than
     * true or false.
     */
    private Truthness equalityTruthness(Object a, Object b) {
        if (a == null || b == null) {
            return UNVALUATABLE;
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

    /**
     * ρ for {@code a < b} on numeric operands. Both are required to be non-null: the caller only
     * reaches this after resolving each side to a present value (a {@code null}/absent operand is
     * filtered out before dispatch). A non-numeric value scores {@link #UNVALUATABLE}, since there is
     * no numeric gradient to compute, but a {@code null} argument is a contract violation, not that case.
     */
    private Truthness numericLessThan(Object a, Object b) {
        Objects.requireNonNull(a, "left operand must not be null");
        Objects.requireNonNull(b, "right operand must not be null");
        Double da = asDouble(a);
        Double db = asDouble(b);
        if (da == null || db == null) {
            return UNVALUATABLE;
        }
        return TruthnessUtils.getLessThanTruthness((double) da, (double) db);
    }

    /**
     * Direct string equality truthness: {@code TRUE} when the strings are equal, otherwise {@code ofTrue}
     * is the left-alignment edit distance scaled from base {@code C} (so it never drops below {@code C})
     * and {@code ofFalse = 1}. Used where a string is compared for equality on its own: relationship
     * types and string-valued property/WHERE equality.
     * <p>
     * Both arguments must be non-null: this is only reached with two resolved strings (a relationship
     * type, or two string values already confirmed present by {@link #equalityTruthness}). A Cypher
     * {@code null} literal never reaches here, since it is handled one level up by
     * {@link #equalityTruthness}.
     */
    public static Truthness stringEqualityTruthness(String a, String b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
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
    public static Truthness stringSimilarityTruthness(String a, String b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        long distance = DistanceHelper.getLeftAlignmentDistance(a, b);
        double ofTrue = 1d - TruthnessUtils.normalizeValue((double) distance);
        return new Truthness(ofTrue, a.equals(b) ? 0d : 1d);
    }

    /**
     * {@code label_in_set(L, labels)}: TRUE if the exact label is present; FALSE if the element has
     * no labels; otherwise the best per-label string similarity scaled from base {@code C}.
     */
    public static Truthness labelInSet(String label, Set<String> labels) {
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

    public static Truthness getStartsWith(String str, String prefix) {
        Objects.requireNonNull(str, "str must not be null");
        Objects.requireNonNull(prefix, "prefix must not be null");
        if (str.startsWith(prefix)) {
            return TRUE_TRUTHNESS;
        }
        int n = Math.min(str.length(), prefix.length());
        String actualPrefix = str.substring(0, n);
        double distance = DistanceHelper.getLeftAlignmentDistance(actualPrefix, prefix);
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(C, distance);
        return new Truthness(h, 1d);
    }

    public static Truthness getEndsWith(String str, String suffix) {
        Objects.requireNonNull(str, "str must not be null");
        Objects.requireNonNull(suffix, "suffix must not be null");
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

    public static Truthness getContains(String str, String substring) {
        Objects.requireNonNull(str, "str must not be null");
        Objects.requireNonNull(substring, "substring must not be null");
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
