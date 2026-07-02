package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.conditions.CypherCondition;
import org.evomaster.client.java.controller.neo4j.conditions.PropertyCondition;
import org.evomaster.client.java.controller.neo4j.conditions.TypeCondition;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.operations.MatchPattern;
import org.evomaster.client.java.controller.neo4j.operations.PatternEdge;
import org.evomaster.client.java.controller.neo4j.operations.PatternNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expands variable-length relationships ({@code *}, {@code +}, {@code *n..m}) to their lower bound:
 * if the lower bound is satisfied the query succeeds, so the upper bound is irrelevant. A lower bound
 * {@code L ≥ 2} becomes a chain of {@code L} cloned edges
 * with {@code L-1} fresh intermediate nodes; those intermediate nodes inherit the labels/properties
 * of both endpoints, and each cloned edge inherits the original edge's type/property conditions.
 * {@code L == 1} is a single ordinary edge; {@code L == 0} merges the two endpoints (a zero-length
 * path identifies them) and drops the edge.
 */
class Neo4jPatternExpander {

    static final class ExpandedQuery {
        final MatchPattern pattern;
        final List<CypherCondition> conditions;

        ExpandedQuery(MatchPattern pattern, List<CypherCondition> conditions) {
            this.pattern = pattern;
            this.conditions = conditions;
        }
    }

    private int counter = 0;

    ExpandedQuery expand(MatchOperation query) {
        MatchPattern pattern = query.getPattern();

        boolean hasVariableLength = false;
        for (PatternEdge e : pattern.getEdges()) {
            if (e.isVariableLength()) {
                hasVariableLength = true;
                break;
            }
        }
        if (!hasVariableLength) {
            return new ExpandedQuery(pattern, query.getConditions());
        }

        List<PatternNode> nodes = new ArrayList<>(pattern.getNodes());
        List<PatternEdge> edges = new ArrayList<>();
        List<CypherCondition> conditions = new ArrayList<>(query.getConditions());
        List<CypherCondition> original = query.getConditions();

        Map<String, String> merges = new HashMap<>();
        Set<String> nodesToRemove = new HashSet<>();

        for (PatternEdge e : pattern.getEdges()) {
            if (!e.isVariableLength()) {
                edges.add(e);
                continue;
            }
            int lower = e.getMinLength() != null ? e.getMinLength() : 0;
            if (lower <= 1) {
                if (lower == 0) {
                    merges.put(e.getTargetVariable(), e.getSourceVariable());
                    nodesToRemove.add(e.getTargetVariable());
                    dropEdgeConditions(conditions, e.getVariableName());
                } else {
                    edges.add(new PatternEdge(e.getVariableName(), e.getSourceVariable(),
                            e.getTargetVariable(), e.isDirected()));
                }
            } else {
                expandChain(e, lower, original, nodes, edges, conditions);
            }
        }

        if (!merges.isEmpty()) {
            Map<String, String> resolved = resolveMerges(merges);
            conditions = renameAll(conditions, resolved);
            edges = renameEndpoints(edges, resolved);
            nodes.removeIf(n -> nodesToRemove.contains(n.getVariableName()));
        }

        return new ExpandedQuery(new MatchPattern(nodes, edges), conditions);
    }

    /**
     * Resolves chained merges to their root, so endpoints identified across several zero-length hops
     * (e.g. {@code (a)-[*0]->(b)-[*0]->(c)} gives {@code b→a, c→b}) all collapse to the same variable
     * rather than to an already-removed intermediate. The cycle guard keeps a pathological pattern that
     * merges a variable back onto itself from looping.
     */
    private Map<String, String> resolveMerges(Map<String, String> merges) {
        Map<String, String> resolved = new HashMap<>();
        for (String variable : merges.keySet()) {
            String root = variable;
            Set<String> seen = new HashSet<>();
            while (merges.containsKey(root) && seen.add(root)) {
                root = merges.get(root);
            }
            resolved.put(variable, root);
        }
        return resolved;
    }

    private void expandChain(PatternEdge edge, int lower, List<CypherCondition> original,
                             List<PatternNode> nodes, List<PatternEdge> edges,
                             List<CypherCondition> conditions) {
        String source = edge.getSourceVariable();
        String target = edge.getTargetVariable();
        String edgeVar = edge.getVariableName();

        List<CypherCondition> srcLabels = endpointConditions(original, source);
        List<CypherCondition> tgtLabels = endpointConditions(original, target);
        List<CypherCondition> edgeConds = edgeConditions(original, edgeVar);

        String prev = source;
        for (int k = 1; k <= lower; k++) {
            boolean last = (k == lower);
            String currentNode = last ? target : freshName("node");
            if (!last) {
                nodes.add(new PatternNode(currentNode));
                // Intermediate node inherits the labels/properties of both endpoints.
                conditions.addAll(renameAll(srcLabels, Collections.singletonMap(source, currentNode)));
                conditions.addAll(renameAll(tgtLabels, Collections.singletonMap(target, currentNode)));
            }
            // First hop reuses the original edge variable (its conditions already apply); later hops
            // get a fresh variable with the edge's type/property conditions cloned onto it.
            String hopVar = (k == 1) ? edgeVar : freshName("rel");
            edges.add(new PatternEdge(hopVar, prev, currentNode, edge.isDirected()));
            if (k > 1) {
                conditions.addAll(renameAll(edgeConds, Collections.singletonMap(edgeVar, hopVar)));
            }
            prev = currentNode;
        }
    }

    /** Label / any-label / property conditions on a node variable (its inheritable structure). */
    private List<CypherCondition> endpointConditions(List<CypherCondition> conditions, String variable) {
        List<CypherCondition> out = new ArrayList<>();
        for (CypherCondition c : conditions) {
            if (ConditionRenamer.referencesVariable(c, variable)) {
                out.add(c);
            }
        }
        return out;
    }

    /** Type / property conditions on an edge variable. */
    private List<CypherCondition> edgeConditions(List<CypherCondition> conditions, String edgeVar) {
        List<CypherCondition> out = new ArrayList<>();
        for (CypherCondition c : conditions) {
            if (c instanceof TypeCondition && edgeVar.equals(((TypeCondition) c).getVariableName())) {
                out.add(c);
            } else if (c instanceof PropertyCondition
                    && edgeVar.equals(((PropertyCondition) c).getVariableName())) {
                out.add(c);
            }
        }
        return out;
    }

    private void dropEdgeConditions(List<CypherCondition> conditions, String edgeVar) {
        conditions.removeAll(edgeConditions(conditions, edgeVar));
    }

    private List<CypherCondition> renameAll(List<CypherCondition> conditions, Map<String, String> renames) {
        List<CypherCondition> out = new ArrayList<>(conditions.size());
        for (CypherCondition c : conditions) {
            out.add(ConditionRenamer.rename(c, renames));
        }
        return out;
    }

    private List<PatternEdge> renameEndpoints(List<PatternEdge> edges, Map<String, String> renames) {
        List<PatternEdge> out = new ArrayList<>(edges.size());
        for (PatternEdge e : edges) {
            String src = renames.getOrDefault(e.getSourceVariable(), e.getSourceVariable());
            String tgt = renames.getOrDefault(e.getTargetVariable(), e.getTargetVariable());
            out.add(new PatternEdge(e.getVariableName(), src, tgt, e.isDirected(),
                    e.isVariableLength(), e.getMinLength(), e.getMaxLength()));
        }
        return out;
    }

    private String freshName(String kind) {
        return "_exp_" + kind + "_" + (counter++);
    }
}
