package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.conditions.CypherCondition;
import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.operations.MatchPattern;
import org.evomaster.client.java.controller.neo4j.operations.PatternEdge;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the search heuristic {@code H(Q, G)} of a parsed Cypher MATCH query {@code Q} against a
 * captured graph {@code G}, returning a {@link Truthness} (how close {@code G} is to satisfying
 * {@code Q}). It works in {@link Truthness} end-to-end, reusing the shared {@link TruthnessUtils}
 * primitives for the aggregations.
 * <p>
 * {@code H(Q, G) = andAggregation(H_match(P_s, G), H_where(C_all, matched_elements(P_s, G)))}, where
 * {@code P_s} is the structural pattern and {@code C_all} the conditions.
 * <p>
 * Expects {@code query} to already be in canonical form: no quantified path patterns and no
 * variable-length edges (both are expanded to a plain pattern by a separate canonization step before
 * reaching this calculator).
 */
public class Neo4jHeuristicsCalculator {

    public static final double C = 0.1;
    public static final Truthness TRUE_TRUTHNESS = new Truthness(1, C);
    public static final Truthness FALSE_TRUTHNESS = new Truthness(C, 1);

    private final Neo4jStructuralMatcher matcher = new Neo4jStructuralMatcher();
    private final Neo4jConditionEvaluator evaluator;

    public Neo4jHeuristicsCalculator() {
        this(null);
    }

    public Neo4jHeuristicsCalculator(TaintHandler taintHandler) {
        this.evaluator = new Neo4jConditionEvaluator(taintHandler);
    }

    Truthness computeHeuristic(MatchOperation query, Neo4jGraph graph) {
        MatchPattern pattern = query.getPattern();
        List<CypherCondition> conditions = query.getConditions();

        List<Neo4jMapping> mappings = matcher.matchedElements(pattern, graph);

        if (query.isOptional() && mappings.isEmpty()) {
            return TRUE_TRUTHNESS;
        }

        Truthness hMatch = computeHeuristicPattern(pattern, graph, mappings);
        Truthness hWhere = computeHeuristicWhere(conditions, mappings);
        return TruthnessUtils.buildAndAggregationTruthness(hMatch, hWhere);
    }

    /**
     * Converts a heuristic to the distance form: {@code 1 - ofTrue}, in
     * {@code [0,1]}, where 0 means the query is satisfied.
     */
    public double computeDistance(MatchOperation query, Neo4jGraph graph) {
        Truthness heuristic = computeHeuristic(query, graph);
        return 1.0d - heuristic.getOfTrue();
    }

    private Truthness computeHeuristicPattern(MatchPattern pattern, Neo4jGraph graph, List<Neo4jMapping> mappings) {
        Truthness nodes = computeHeuristicMatchNodes(pattern.nodeCount(), graph.nodeCount());
        Truthness edges = computeHeuristicMatchEdges(pattern.getEdges(), graph, mappings);
        return TruthnessUtils.buildAndAggregationTruthness(nodes, edges);
    }

    /**
     * Count-based node availability: enough graph nodes to bind the pattern's nodes. Pure cardinality,
     * no label/property check (those are conditions evaluated by H_where).
     */
    Truthness computeHeuristicMatchNodes(int required, int available) {
        if (required < 0 || available < 0) {
            throw new IllegalArgumentException(
                    "node counts must be non-negative, got required=" + required + ", available=" + available);
        }
        if (required == 0) {
            return TRUE_TRUTHNESS;
        }
        if (available == 0) {
            return FALSE_TRUTHNESS;
        }
        if (available >= required) {
            return TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(C, (double) available / required);
    }

    /**
     * Edge availability: the best, over all node mappings, of whether every pattern edge has a
     * matching graph relationship under that mapping. Empty edge set is vacuously satisfied.
     */
    private Truthness computeHeuristicMatchEdges(List<PatternEdge> patternEdges, Neo4jGraph graph,
                                                 List<Neo4jMapping> mappings) {
        if (mappings.isEmpty()) {
            return FALSE_TRUTHNESS;
        }
        if (patternEdges.isEmpty()) {
            return TRUE_TRUTHNESS;
        }
        // matched_elements already binds every pattern edge to an existing relationship, so edgeMatch is
        // TRUE for any mapping here; the per-mapping scaled form is kept for the general case and stays
        // correct should the matcher ever yield node-only mappings.
        double maxOfTrue = 0d;
        for (Neo4jMapping mapping : mappings) {
            Truthness t = edgesForMapping(patternEdges, graph, mapping);
            if (t.isTrue()) {
                return TRUE_TRUTHNESS;
            }
            if (t.getOfTrue() > maxOfTrue) {
                maxOfTrue = t.getOfTrue();
            }
        }
        return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
    }

    private Truthness edgesForMapping(List<PatternEdge> patternEdges, Neo4jGraph graph,
                                      Neo4jMapping mapping) {
        Truthness[] perEdge = new Truthness[patternEdges.size()];
        for (int i = 0; i < patternEdges.size(); i++) {
            perEdge[i] = edgeMatch(patternEdges.get(i), graph, mapping);
        }
        return TruthnessUtils.buildAndAggregationTruthness(perEdge);
    }

    /** Existence-only edge check: TRUE if some graph relationship matches the edge's endpoints. */
    private Truthness edgeMatch(PatternEdge edge, Neo4jGraph graph, Neo4jMapping mapping) {
        Neo4jNode source = mapping.getNode(edge.getSourceVariable());
        Neo4jNode target = mapping.getNode(edge.getTargetVariable());
        if (source == null || target == null) {
            return FALSE_TRUTHNESS;
        }
        for (Neo4jEdge rel : graph.getEdges()) {
            boolean forward = rel.getSourceId().equals(source.getId())
                    && rel.getTargetId().equals(target.getId());
            boolean backward = !edge.isDirected()
                    && rel.getSourceId().equals(target.getId())
                    && rel.getTargetId().equals(source.getId());
            if (forward || backward) {
                return TRUE_TRUTHNESS;
            }
        }
        return FALSE_TRUTHNESS;
    }

    /**
     * Best, over all matched elements, of how well the conditions hold; if no mapping satisfies them
     * fully, the best partial score scaled from base {@code C}. No mappings means the structure was
     * absent, so the conditions cannot hold: FALSE.
     */
    private Truthness computeHeuristicWhere(List<CypherCondition> conditions, List<Neo4jMapping> mappings) {
        if (mappings.isEmpty()) {
            return FALSE_TRUTHNESS;
        }
        double maxOfTrue = 0d;
        for (Neo4jMapping mapping : mappings) {
            Truthness t = matchConditions(conditions, mapping);
            if (t.isTrue()) {
                return TRUE_TRUTHNESS;
            }
            if (t.getOfTrue() > maxOfTrue) {
                maxOfTrue = t.getOfTrue();
            }
        }
        return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
    }

    /**
     * AND-aggregates the truthness of every condition under one mapping. Conditions that cannot be
     * valuated (absent property, opaque/raw) are skipped; if none remain, the mapping vacuously
     * satisfies the (empty) constraint set.
     */
    private Truthness matchConditions(List<CypherCondition> conditions, Neo4jMapping mapping) {
        List<Truthness> truths = new ArrayList<>();
        for (CypherCondition c : conditions) {
            Truthness t = evaluator.evaluateCondition(c, mapping);
            if (t != null) {
                truths.add(t);
            }
        }
        if (truths.isEmpty()) {
            return TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildAndAggregationTruthness(truths.toArray(new Truthness[0]));
    }
}
