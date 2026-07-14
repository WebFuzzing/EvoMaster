package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.conditions.CypherCondition;
import org.evomaster.client.java.controller.neo4j.conditions.PropertyCondition;
import org.evomaster.client.java.controller.neo4j.conditions.TypeCondition;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.operations.MatchPattern;
import org.evomaster.client.java.controller.neo4j.operations.PatternEdge;
import org.evomaster.client.java.controller.neo4j.operations.PatternNode;
import org.evomaster.client.java.controller.neo4j.operations.QuantifiedPathPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flattens a {@link MatchPattern} into one with no variable-length relationships and no quantified
 * path patterns (QPPs), so the rest of the calculator only ever deals with plain nodes and edges.
 * Two independent expansions run in sequence, each to the construct's lower bound {@code L} (if the
 * lower bound is satisfied the query succeeds, so the upper bound is irrelevant):
 * <ol>
 *     <li>{@link #expandQuantifiedPaths}: every QPP ({@code ((a)-[]->(b)){1,3}}) is replaced by
 *     {@code L} clones of its sub-pattern, chained end to end and spliced onto the outer nodes it sits
 *     between (see {@link QuantifiedPathPattern#getEntryVariable()}/{@link QuantifiedPathPattern#getExitVariable()}).
 *     Runs first so its output — plain nodes/edges, some possibly still variable-length — feeds the
 *     next step.</li>
 *     <li>The pre-existing variable-length-edge expansion ({@code *}, {@code +}, {@code *n..m}): a
 *     lower bound {@code L ≥ 2} becomes a chain of {@code L} cloned edges with {@code L-1} fresh
 *     intermediate nodes inheriting both endpoints' labels/properties; {@code L == 1} is a single
 *     ordinary edge; {@code L == 0} merges the two endpoints (a zero-length path identifies them) and
 *     drops the edge. This also expands variable-length edges that were themselves inside a QPP's
 *     sub-pattern, since step 1 clones them unchanged (min/max preserved) into the flat edge list.</li>
 * </ol>
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
        ExpandedQuery afterQpp = expandQuantifiedPaths(query.getPattern(), query.getConditions());
        return expandVariableLengthEdges(afterQpp.pattern, afterQpp.conditions);
    }

    private ExpandedQuery expandVariableLengthEdges(MatchPattern pattern, List<CypherCondition> conditions) {
        boolean hasVariableLength = false;
        for (PatternEdge e : pattern.getEdges()) {
            if (e.isVariableLength()) {
                hasVariableLength = true;
                break;
            }
        }
        if (!hasVariableLength) {
            return new ExpandedQuery(pattern, conditions);
        }

        List<PatternNode> nodes = new ArrayList<>(pattern.getNodes());
        List<PatternEdge> edges = new ArrayList<>();
        List<CypherCondition> original = conditions;
        List<CypherCondition> working = new ArrayList<>(conditions);

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
                    dropEdgeConditions(working, e.getVariableName());
                } else {
                    edges.add(new PatternEdge(e.getVariableName(), e.getSourceVariable(),
                            e.getTargetVariable(), e.isDirected()));
                }
            } else {
                expandChain(e, lower, original, nodes, edges, working);
            }
        }

        if (!merges.isEmpty()) {
            Map<String, String> resolved = resolveMerges(merges);
            working = renameAll(working, resolved);
            edges = renameEndpoints(edges, resolved);
            nodes.removeIf(n -> nodesToRemove.contains(n.getVariableName()));
        }

        return new ExpandedQuery(new MatchPattern(nodes, edges), working);
    }

    /**
     * Replaces every QPP in {@code pattern} with {@code L} concrete clones of its sub-pattern (see
     * {@link #expandQpp}), recursing into nested QPPs first so each clone is built from an
     * already-flattened sub-pattern. A pattern with no QPPs is returned unchanged.
     */
    private ExpandedQuery expandQuantifiedPaths(MatchPattern pattern, List<CypherCondition> conditions) {
        if (pattern.getQuantifiedPaths().isEmpty()) {
            return new ExpandedQuery(pattern, conditions);
        }

        List<PatternNode> nodes = new ArrayList<>(pattern.getNodes());
        List<PatternEdge> edges = new ArrayList<>(pattern.getEdges());
        List<CypherCondition> working = new ArrayList<>(conditions);

        Map<String, String> merges = new HashMap<>();
        Set<String> nodesToRemove = new HashSet<>();

        for (QuantifiedPathPattern qpp : pattern.getQuantifiedPaths()) {
            expandQpp(qpp, nodes, edges, working, merges, nodesToRemove);
        }

        if (!merges.isEmpty()) {
            Map<String, String> resolved = resolveMerges(merges);
            working = renameAll(working, resolved);
            edges = renameEndpoints(edges, resolved);
            nodes.removeIf(n -> nodesToRemove.contains(n.getVariableName()));
        }

        return new ExpandedQuery(new MatchPattern(nodes, edges), working);
    }

    /**
     * Expands one QPP to its lower bound {@code L}, splicing onto {@link QuantifiedPathPattern#getEntryVariable()}
     * / {@link QuantifiedPathPattern#getExitVariable()} (either may be {@code null} — the QPP opens or
     * closes the whole pattern, or is the whole pattern). {@code L == 0} merges entry and exit (when
     * both exist) exactly like a zero-length edge, via the same {@code merges} map so it composes with
     * plain zero-length edges resolved later; the sub-pattern's own conditions never materialize.
     * {@code L ≥ 1} clones the sub-pattern {@code L} times, chaining each copy's own exit-side node to
     * the next copy's own entry-side node with a fresh name, and binding the first copy's entry-side
     * and the last copy's exit-side to the outer variables when present.
     */
    private void expandQpp(QuantifiedPathPattern qpp, List<PatternNode> nodes, List<PatternEdge> edges,
                           List<CypherCondition> conditions, Map<String, String> merges,
                           Set<String> nodesToRemove) {
        ExpandedQuery flatSub = expandQuantifiedPaths(qpp.getSubPattern(), qpp.getConditions());
        List<PatternNode> subNodes = flatSub.pattern.getNodes();
        if (subNodes.isEmpty()) {
            return;
        }
        // The sub-pattern's own boundary is the source of its first edge and the target of its last
        // edge, in source-text order — not the first/last *distinct* node variable, which would pick
        // the wrong endpoint when the sub-pattern cycles back onto an earlier variable (e.g.
        // (a)-[:R]->(b)-[:S]->(a) must exit on "a", not "b", the last newly-introduced name).
        List<PatternEdge> subEdges = flatSub.pattern.getEdges();
        String subEntry = subEdges.isEmpty()
                ? subNodes.get(0).getVariableName()
                : subEdges.get(0).getSourceVariable();
        String subExit = subEdges.isEmpty()
                ? subNodes.get(subNodes.size() - 1).getVariableName()
                : subEdges.get(subEdges.size() - 1).getTargetVariable();

        String outerEntry = qpp.getEntryVariable();
        String outerExit = qpp.getExitVariable();
        int lower = qpp.getMin();

        if (lower == 0) {
            if (outerEntry != null && outerExit != null) {
                merges.put(outerExit, outerEntry);
                nodesToRemove.add(outerExit);
            }
            return;
        }

        String prevExit = outerEntry;
        for (int rep = 1; rep <= lower; rep++) {
            boolean last = (rep == lower);
            Map<String, String> boundary = new HashMap<>();

            String entryName = prevExit != null ? prevExit : subEntry;
            boundary.put(subEntry, entryName);

            String exitName;
            if (subEntry.equals(subExit)) {
                // The sub-pattern's own entry and exit are the same variable (a single bare node, or a
                // cyclic sub-pattern that returns to its own start): this repetition begins and ends on
                // the same node. If that's also the last repetition and there's an outer exit variable,
                // the outer exit is really an alias for the same node too — merge it in transitively,
                // the same way a zero-length QPP merges its entry and exit.
                exitName = entryName;
                if (last && outerExit != null && !outerExit.equals(exitName)) {
                    merges.put(outerExit, exitName);
                    nodesToRemove.add(outerExit);
                }
            } else if (last && outerExit != null) {
                exitName = outerExit;
            } else {
                exitName = freshName("qpp_node");
            }
            boundary.put(subExit, exitName);

            cloneSubPatternInto(flatSub.pattern, flatSub.conditions, boundary, nodes, edges, conditions);
            prevExit = exitName;
        }
    }

    /**
     * Adds one renamed clone of {@code subPattern} (nodes, edges, conditions) into the accumulating
     * outer lists. {@code boundary} pins the sub-pattern's own entry/exit node names to their bound
     * names for this repetition; every other node/edge variable gets a fresh globally-unique name so
     * repetitions never collide. A node already present in {@code nodes} (an outer node, or the
     * previous repetition's exit) is not re-added.
     */
    private void cloneSubPatternInto(MatchPattern subPattern, List<CypherCondition> subConditions,
                                     Map<String, String> boundary, List<PatternNode> nodes,
                                     List<PatternEdge> edges, List<CypherCondition> conditions) {
        Map<String, String> renames = new HashMap<>(boundary);
        for (PatternNode n : subPattern.getNodes()) {
            renames.putIfAbsent(n.getVariableName(), freshName("qpp_node"));
        }
        for (PatternEdge e : subPattern.getEdges()) {
            if (e.getVariableName() != null) {
                renames.putIfAbsent(e.getVariableName(), freshName("qpp_rel"));
            }
        }

        for (PatternNode n : subPattern.getNodes()) {
            String newName = renames.get(n.getVariableName());
            if (!containsNode(nodes, newName)) {
                nodes.add(new PatternNode(newName));
            }
        }
        for (PatternEdge e : subPattern.getEdges()) {
            String src = renames.getOrDefault(e.getSourceVariable(), e.getSourceVariable());
            String tgt = renames.getOrDefault(e.getTargetVariable(), e.getTargetVariable());
            String var = e.getVariableName() != null ? renames.get(e.getVariableName()) : null;
            edges.add(new PatternEdge(var, src, tgt, e.isDirected(),
                    e.isVariableLength(), e.getMinLength(), e.getMaxLength()));
        }
        for (CypherCondition c : subConditions) {
            conditions.add(ConditionRenamer.rename(c, renames));
        }
    }

    private boolean containsNode(List<PatternNode> nodes, String variableName) {
        for (PatternNode n : nodes) {
            if (n.getVariableName().equals(variableName)) {
                return true;
            }
        }
        return false;
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
