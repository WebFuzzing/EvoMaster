package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.parser.CypherParser;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserException;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserFactory;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates {@link Neo4jHeuristicsCalculator} end-to-end (parser -> calculator) against two worked
 * examples, plus unit checks of the building blocks. Queries here are already in canonical form (no
 * quantified path patterns, no variable-length edges) — that expansion is a separate, later step.
 * <p>
 * Note on exact values: the heuristic calls EvoMaster's real {@code TruthnessUtils} (equality
 * {@code 1/(1+d)}, less-than {@code 1/(1.1+d)}, both un-based), so the tests assert the real computed
 * value and, more importantly, the qualitative structure and the gradient (a one-property mutation
 * flips the query to satisfied).
 */
class Neo4jHeuristicsCalculatorTest {

    private static final double DELTA = 1e-9;

    private final CypherParser parser = CypherParserFactory.buildParser();
    private final Neo4jHeuristicsCalculator calculator = new Neo4jHeuristicsCalculator();

    private static final String EXAMPLE_QUERY =
            "MATCH (a:Person {age: 25})-[r:KNOWS]->(b:Person) WHERE b.age > 30 RETURN b";

    private Neo4jGraph example1Graph(int n2Age) {
        Neo4jNode n1 = node("n1", labels("Person"), props("age", 25, "name", "Ana"));
        Neo4jNode n2 = node("n2", labels("Person"), props("age", n2Age, "name", "Luis"));
        Neo4jNode n3 = node("n3", labels("Animal"), props("age", 5, "name", "Rex"));
        Neo4jNode n4 = node("n4", labels("Person"), props("age", 40, "name", "Carlos"));
        Neo4jEdge e1 = rel("e1", "KNOWS", "n1", "n2");
        Neo4jEdge e2 = rel("e2", "LIKES", "n1", "n3");
        Neo4jEdge e3 = rel("e3", "KNOWS", "n3", "n4");
        return new Neo4jGraph(Arrays.asList(n1, n2, n3, n4), Arrays.asList(e1, e2, e3));
    }

    @Test
    void testExample1StructuralMappings() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        assertEquals(3, new Neo4jStructuralMatcher()
                .matchedElements(q.getPattern(), example1Graph(28)).size());
    }

    @Test
    void testExample1NotSatisfiedButStrongGradient() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        Truthness h = calculator.computeHeuristic(q, example1Graph(28));

        // Not satisfied: b.age (28) is not > 30 on any mapping.
        assertFalse(h.isTrue());
        assertEquals(1.0, h.getOfFalse(), DELTA);
        // Structure fully available and the best mapping satisfies 4 of 5 conditions: close to satisfied.
        assertTrue(h.getOfTrue() > 0.9);
    }

    @Test
    void testComputeDistanceMirrorsHeuristic() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        Neo4jGraph g = example1Graph(28);
        // The distance reported to the fitness DTO is 1 - ofTrue, and is positive for an unsatisfied query.
        double distance = calculator.computeDistance(q, g);
        assertEquals(1.0 - calculator.computeHeuristic(q, g).getOfTrue(), distance, DELTA);
        assertTrue(distance > 0);
    }

    @Test
    void testExample1SatisfiedAfterAgeMutation() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        // Mutating Luis age 28 -> 31 makes the best mapping (a->n1, b->n2) fully satisfy the query.
        Truthness h = calculator.computeHeuristic(q, example1Graph(31));
        assertTrue(h.isTrue());
        assertEquals(1.0, h.getOfTrue(), DELTA);
    }

    // Worked example 2: partial match, clear gradient

    private Neo4jGraph example2Graph() {
        Neo4jNode n1 = node("n1", labels("Person"), props("age", 27, "name", "Ana"));
        Neo4jNode n2 = node("n2", labels("Person"), props("age", 35, "name", "Luis"));
        Neo4jNode n3 = node("n3", labels("Person"), props("age", 22, "name", "Maria"));
        Neo4jEdge e1 = rel("e1", "LIKES", "n1", "n2");
        Neo4jEdge e2 = rel("e2", "KNOWS", "n2", "n3");
        return new Neo4jGraph(Arrays.asList(n1, n2, n3), Arrays.asList(e1, e2));
    }

    @Test
    void testExample2PartialMatch() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        Truthness h = calculator.computeHeuristic(q, example2Graph());

        assertFalse(h.isTrue());
        assertEquals(1.0, h.getOfFalse(), DELTA);
        // Partial match: a clear gradient, well above the unsatisfiable floor.
        assertTrue(h.getOfTrue() > 0.8);
    }

    @Test
    void testExample2SatisfiedOnTunedGraph() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        // a:Person{age:25} -KNOWS-> b:Person{age:40>30}
        Neo4jNode a = node("a", labels("Person"), props("age", 25));
        Neo4jNode b = node("b", labels("Person"), props("age", 40));
        Neo4jGraph g = new Neo4jGraph(Arrays.asList(a, b),
                Collections.singletonList(rel("e", "KNOWS", "a", "b")));
        assertTrue(calculator.computeHeuristic(q, g).isTrue());
    }

    @Test
    void testNoStructuralMatchStillScoresNodesByCount() throws CypherParserException {
        MatchOperation q = parser.parse(EXAMPLE_QUERY);
        // Right nodes exist but there is no relationship: matched_elements is empty, so H_where and
        // H_match_edges are FALSE, but H_match_nodes is still TRUE (2 needed ≤ 2 available).
        Neo4jNode a = node("a", labels("Person"), props("age", 25));
        Neo4jNode b = node("b", labels("Person"), props("age", 40));
        Neo4jGraph g = new Neo4jGraph(Arrays.asList(a, b), Collections.emptyList());
        Truthness h = calculator.computeHeuristic(q, g);
        assertFalse(h.isTrue());
        assertEquals(1.0, h.getOfFalse(), DELTA);
        assertTrue(h.getOfTrue() > Neo4jHeuristicsCalculator.C);
    }

    @Test
    void testHMatchNodesCountBased() {
        // enough nodes -> TRUE
        assertTrue(calculator.computeHeuristicMatchNodes(2, 4).isTrue());
        assertTrue(calculator.computeHeuristicMatchNodes(2, 2).isTrue());
        // no nodes in graph -> FALSE
        assertEquals(1.0, calculator.computeHeuristicMatchNodes(2, 0).getOfFalse(), DELTA);
        assertFalse(calculator.computeHeuristicMatchNodes(2, 0).isTrue());
        // no nodes required -> TRUE
        assertTrue(calculator.computeHeuristicMatchNodes(0, 0).isTrue());
        // partial availability -> scaled in (C, 1)
        Truthness partial = calculator.computeHeuristicMatchNodes(4, 2);
        assertEquals(1.0, partial.getOfFalse(), DELTA);
        assertEquals(Neo4jHeuristicsCalculator.C + 0.9 * (2.0 / 4.0), partial.getOfTrue(), DELTA);
    }

    @Test
    void testStringEqualityTruthness() {
        assertTrue(Neo4jConditionEvaluator.stringEqualityTruthness("KNOWS", "KNOWS").isTrue());
        Truthness diff = Neo4jConditionEvaluator.stringEqualityTruthness("KNOWS", "LIKES");
        assertEquals(1.0, diff.getOfFalse(), DELTA);
        assertTrue(diff.getOfTrue() < 1.0);
    }

    @Test
    void testLabelInSet() {
        assertTrue(Neo4jConditionEvaluator.labelInSet("Person", labels("Person")).isTrue());
        assertEquals(1.0,
                Neo4jConditionEvaluator.labelInSet("Person", labels()).getOfFalse(), DELTA);
        assertFalse(Neo4jConditionEvaluator.labelInSet("Person", labels()).isTrue());
        // present but different label: scaled, never reaches 1
        Truthness t = Neo4jConditionEvaluator.labelInSet("Person", labels("Animal"));
        assertEquals(1.0, t.getOfFalse(), DELTA);
        assertTrue(t.getOfTrue() >= Neo4jHeuristicsCalculator.C && t.getOfTrue() < 1.0);
    }

    @Test
    void testStartsWith() {
        assertTrue(Neo4jConditionEvaluator.getStartsWith("hello", "hel").isTrue());
        Truthness t = Neo4jConditionEvaluator.getStartsWith("hello", "xyz");
        assertEquals(1.0, t.getOfFalse(), DELTA);
        assertTrue(t.getOfTrue() >= Neo4jHeuristicsCalculator.C && t.getOfTrue() < 1.0);
    }

    @Test
    void testEqualityAgainstNullLiteralIsNotSatisfied() throws CypherParserException {
        // Cypher's null semantics: any comparison against null is null (unknown), never true or
        // false, so `= null` is unsatisfiable and must not be scored as satisfied.
        Neo4jNode a = node("a", labels("Person"), props("name", "Ann"));
        Neo4jNode b = node("b", labels("Person"), props("name", "Bob"));
        Neo4jGraph g = new Neo4jGraph(Arrays.asList(a, b),
                Collections.singletonList(rel("e", "KNOWS", "a", "b")));

        Truthness baseline = calculator.computeHeuristic(
                parser.parse("MATCH (a)-[r]->(b) RETURN b"), g);
        Truthness whereNull = calculator.computeHeuristic(
                parser.parse("MATCH (a)-[r]->(b) WHERE b.name = null RETURN b"), g);
        Truthness inlineNull = calculator.computeHeuristic(
                parser.parse("MATCH (a)-[r]->(b {name: null}) RETURN b"), g);

        assertTrue(baseline.isTrue());
        assertFalse(whereNull.isTrue());
        assertFalse(inlineNull.isTrue());
        // Unvaluatable, so scored below the unconstrained baseline, but still above zero so the rest
        // of the query keeps guiding the search.
        assertTrue(whereNull.getOfTrue() < baseline.getOfTrue());
        assertTrue(inlineNull.getOfTrue() < baseline.getOfTrue());
        assertTrue(whereNull.getOfTrue() > 0);
    }

    /** A single {@code (:Person {age: 25})} node, no relationships. */
    private Neo4jGraph singlePersonAged25() {
        return new Neo4jGraph(
                Collections.singletonList(node("n1", labels("Person"), props("age", 25))),
                Collections.emptyList());
    }

    @Test
    void testOrWithUnvaluatableBranchIsSatisfiedByTheOtherBranch() throws CypherParserException {
        // Cypher: `true OR null` is `true`, and Neo4j does return the node, so the query is satisfied
        // even though the nickname branch cannot be valuated.
        MatchOperation q = parser.parse(
                "MATCH (p:Person) WHERE p.age = 25 OR p.nickname = \"bob\" RETURN p");
        Truthness h = calculator.computeHeuristic(q, singlePersonAged25());

        assertTrue(h.isTrue());
        assertEquals(0.0, calculator.computeDistance(q, singlePersonAged25()), DELTA);
    }

    @Test
    void testAndWithUnvaluatableBranchIsNotSatisfied() throws CypherParserException {
        // Cypher: `true AND null` is `null`, so the query matches nothing. Skipping the unvaluatable
        // branch used to report distance 0 here, telling the search it was already done.
        MatchOperation q = parser.parse(
                "MATCH (p:Person) WHERE p.age = 25 AND p.nickname = \"bob\" RETURN p");
        Truthness h = calculator.computeHeuristic(q, singlePersonAged25());

        assertFalse(h.isTrue());
        assertTrue(calculator.computeDistance(q, singlePersonAged25()) > 0);
        // The satisfied branch still contributes, so this stays well above the unsatisfiable floor.
        assertTrue(h.getOfTrue() > Neo4jHeuristicsCalculator.C);
    }

    @Test
    void testOnlyUnvaluatableConditionIsNotSatisfied() throws CypherParserException {
        // The absent property is the only WHERE condition: nothing matches in Neo4j, so the query
        // must not be scored as satisfied just because the label holds.
        MatchOperation q = parser.parse("MATCH (p:Person) WHERE p.nickname = \"bob\" RETURN p");
        Truthness h = calculator.computeHeuristic(q, singlePersonAged25());

        assertFalse(h.isTrue());
        assertTrue(calculator.computeDistance(q, singlePersonAged25()) > 0);
    }

    @Test
    void testDivisionByZeroIsNotSatisfiedAndDoesNotThrow() throws CypherParserException {
        // A zero divisor makes the arithmetic unresolvable, so the comparison is unvaluatable rather
        // than an error: the score has to come back low, not blow up mid-search.
        MatchOperation q = parser.parse("MATCH (p:Person) WHERE p.age / 0 = 5 RETURN p");
        Truthness h = calculator.computeHeuristic(q, singlePersonAged25());

        assertFalse(h.isTrue());
        assertTrue(calculator.computeDistance(q, singlePersonAged25()) > 0);
    }

    @Test
    void testModuloByZeroIsNotSatisfiedAndDoesNotThrow() throws CypherParserException {
        MatchOperation q = parser.parse("MATCH (p:Person) WHERE p.age % 0 = 5 RETURN p");
        Truthness h = calculator.computeHeuristic(q, singlePersonAged25());

        assertFalse(h.isTrue());
        assertTrue(calculator.computeDistance(q, singlePersonAged25()) > 0);
    }

    @Test
    void testDivisionByZeroBetweenLiteralsDoesNotThrow() throws CypherParserException {
        // Both sides are literals, so this one has to survive constant folding too.
        MatchOperation q = parser.parse("MATCH (p:Person) WHERE 25 / 0 = 5 RETURN p");

        assertFalse(calculator.computeHeuristic(q, singlePersonAged25()).isTrue());
    }

    @Test
    void testDivisionByANonZeroDivisorIsStillEvaluated() throws CypherParserException {
        // Guards the two tests above from passing for the wrong reason: ordinary division still works.
        MatchOperation q = parser.parse("MATCH (p:Person) WHERE p.age / 5 = 5 RETURN p");

        assertEquals(0.0, calculator.computeDistance(q, singlePersonAged25()), DELTA);
    }

    @Test
    void testNotOfUnvaluatableConditionIsNotSatisfied() throws CypherParserException {
        // Cypher: `NOT null` is `null`, not true. Inverting the unvaluatable score would flip its low
        // ofTrue into a near-true one and invent a match that Neo4j does not return.
        MatchOperation q = parser.parse("MATCH (p:Person) WHERE NOT (p.nickname = \"bob\") RETURN p");
        Truthness h = calculator.computeHeuristic(q, singlePersonAged25());

        assertFalse(h.isTrue());
        assertTrue(calculator.computeDistance(q, singlePersonAged25()) > 0);
    }

    @Test
    void testUnvaluatableConditionKeepsTheGradientOfItsSiblings() throws CypherParserException {
        // The point of scoring instead of flattening: the absent nickname must not stop the search
        // from learning that moving age towards 25 is an improvement.
        MatchOperation q = parser.parse(
                "MATCH (p:Person) WHERE p.age = 25 AND p.nickname = \"bob\" RETURN p");

        Neo4jGraph far = new Neo4jGraph(
                Collections.singletonList(node("n1", labels("Person"), props("age", 40))),
                Collections.emptyList());
        Neo4jGraph near = new Neo4jGraph(
                Collections.singletonList(node("n1", labels("Person"), props("age", 27))),
                Collections.emptyList());

        assertTrue(calculator.computeHeuristic(q, near).getOfTrue()
                > calculator.computeHeuristic(q, far).getOfTrue());
        assertTrue(calculator.computeHeuristic(q, singlePersonAged25()).getOfTrue()
                > calculator.computeHeuristic(q, near).getOfTrue());
    }

    private static Neo4jNode node(String id, Set<String> labels, Map<String, Object> props) {
        return new Neo4jNode(id, labels, props);
    }

    private static Neo4jEdge rel(String id, String type, String from, String to) {
        return new Neo4jEdge(id, type, from, to, Collections.emptyMap());
    }

    private static Set<String> labels(String... ls) {
        return new LinkedHashSet<>(Arrays.asList(ls));
    }

    private static Map<String, Object> props(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
