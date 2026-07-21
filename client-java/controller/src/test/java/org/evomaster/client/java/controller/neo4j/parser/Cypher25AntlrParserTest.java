package org.evomaster.client.java.controller.neo4j.parser;

import org.evomaster.client.java.controller.neo4j.conditions.*;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;
import org.evomaster.client.java.controller.neo4j.operations.PatternEdge;
import org.evomaster.client.java.controller.neo4j.operations.QuantifiedPathPattern;
import org.evomaster.client.java.controller.neo4j.parser.cypher25.Cypher25AntlrParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Cypher25AntlrParser}, the parser built on the official Neo4j
 * Cypher25 grammar. Invalid input fails with a {@link CypherParserException}, and label/type
 * expressions are preserved as a faithful boolean tree ({@link OrCondition}/{@link AndCondition}/
 * {@link NotCondition} over {@link LabelCondition}/{@link TypeCondition} leaves), the same
 * structure used for the WHERE clause.
 */
class Cypher25AntlrParserTest {

    private final CypherParser parser = CypherParserFactory.buildParser();

    private MatchOperation parse(String query) {
        try {
            return parser.parse(query);
        } catch (CypherParserException e) {
            throw new AssertionError("Expected a successful parse for: " + query, e);
        }
    }

    private void assertFails(String query) {
        assertThrows(CypherParserException.class, () -> parser.parse(query));
    }

    private <T extends CypherCondition> long countOf(MatchOperation op, Class<T> type) {
        return op.getConditions().stream().filter(type::isInstance).count();
    }

    private ComparisonCondition comparison(MatchOperation op) {
        return (ComparisonCondition) op.getConditions().stream()
                .filter(ComparisonCondition.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no ComparisonCondition found"));
    }

    @Test
    void testAllNodes() {
        MatchOperation op = parse("MATCH (n) RETURN n");
        assertEquals(1, op.getPattern().nodeCount());
        assertEquals(0, op.getPattern().edgeCount());
        assertEquals(0, op.getConditions().size());
        assertEquals("n", op.getPattern().getNodes().get(0).getVariableName());
    }

    @Test
    void testNodeWithLabel() {
        MatchOperation op = parse("MATCH (movie:Movie) RETURN movie.title");
        assertEquals(1, op.getPattern().nodeCount());
        assertEquals(1, op.getConditions().size());
        LabelCondition label = (LabelCondition) op.getConditions().get(0);
        assertEquals("movie", label.getVariableName());
        assertEquals("Movie", label.getLabel());
    }

    @Test
    void testNodeWithMultipleLabelsAnd() {
        MatchOperation op = parse("MATCH (e:Person:Employee) RETURN e");
        AndCondition and = (AndCondition) op.getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().stream().allMatch(LabelCondition.class::isInstance));
    }

    @Test
    void testNodeWithMultipleLabelsAmpersand() {
        MatchOperation op = parse("MATCH (e:Person&Employee) RETURN e");
        AndCondition and = (AndCondition) op.getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().stream().allMatch(LabelCondition.class::isInstance));
    }

    @Test
    void testLabelExpressionOr() {
        MatchOperation op = parse("MATCH (n:Movie|Person) RETURN n.name");
        OrCondition or = (OrCondition) op.getConditions().get(0);
        assertEquals(2, or.getConditions().size());
        assertTrue(or.getConditions().stream().allMatch(LabelCondition.class::isInstance));
        assertEquals("n", ((LabelCondition) or.getConditions().get(0)).getVariableName());
        assertTrue(or.getConditions().stream()
                .anyMatch(c -> "Movie".equals(((LabelCondition) c).getLabel())));
        assertTrue(or.getConditions().stream()
                .anyMatch(c -> "Person".equals(((LabelCondition) c).getLabel())));
    }

    @Test
    void testNodeLabelNegation() {
        // (n:!Person): the node must NOT have the label.
        MatchOperation op = parse("MATCH (n:!Person) RETURN n");
        NotCondition not = (NotCondition) op.getConditions().get(0);
        assertEquals("Person", ((LabelCondition) not.getCondition()).getLabel());
    }

    @Test
    void testNodeLabelMixedAndOr() {
        // (n:A&B|C) parses as (A AND B) OR C; & binds tighter than |.
        MatchOperation op = parse("MATCH (n:A&B|C) RETURN n");
        OrCondition or = (OrCondition) op.getConditions().get(0);
        assertEquals(2, or.getConditions().size());
        AndCondition and = (AndCondition) or.getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        assertInstanceOf(LabelCondition.class, or.getConditions().get(1));
    }

    @Test
    void testNodeLabelParenthesizedRegroup() {
        // (n:A&(B|C)) regroups to A AND (B OR C).
        MatchOperation op = parse("MATCH (n:A&(B|C)) RETURN n");
        AndCondition and = (AndCondition) op.getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        assertInstanceOf(LabelCondition.class, and.getConditions().get(0));
        assertEquals(2, ((OrCondition) and.getConditions().get(1)).getConditions().size());
    }

    @Test
    void testNodeAnyLabelWildcard() {
        MatchOperation op = parse("MATCH (n:%) RETURN n");
        AnyLabelCondition any = (AnyLabelCondition) op.getConditions().get(0);
        assertEquals("n", any.getVariableName());
    }

    @Test
    void testNodeIsLabel() {
        // The GQL 'IS' form is equivalent to the colon form.
        MatchOperation op = parse("MATCH (n IS Person) RETURN n");
        LabelCondition label = (LabelCondition) op.getConditions().get(0);
        assertEquals("n", label.getVariableName());
        assertEquals("Person", label.getLabel());
    }

    @Test
    void testNodeIsLabelExpression() {
        // The 'IS' form supports the same &|! operators as the colon form.
        MatchOperation op = parse("MATCH (n IS A&B|C) RETURN n");
        OrCondition or = (OrCondition) op.getConditions().get(0);
        assertEquals(2, or.getConditions().size());
        assertEquals(2, ((AndCondition) or.getConditions().get(0)).getConditions().size());
    }

    @Test
    void testRelationshipIsType() {
        MatchOperation op = parse("MATCH (a)-[r IS KNOWS]->(b) RETURN a, b");
        TypeCondition type = (TypeCondition) op.getConditions().stream()
                .filter(TypeCondition.class::isInstance).findFirst()
                .orElseThrow(() -> new AssertionError("no TypeCondition for IS relationship type"));
        assertEquals("KNOWS", type.getType());
    }

    @Test
    void testNodeInlineWhere() {
        MatchOperation op = parse("MATCH (n WHERE n.age > 18) RETURN n");
        ComparisonCondition cc = comparison(op);
        assertEquals(ComparisonOperator.GREATER_THAN, cc.getOperator());
        assertEquals("age", ((PropertyOperand) cc.getLeft()).getPropertyKey());
    }

    @Test
    void testRelationshipInlineWhere() {
        MatchOperation op = parse("MATCH (a)-[r WHERE r.weight > 5]->(b) RETURN a");
        ComparisonCondition cc = comparison(op);
        assertEquals("weight", ((PropertyOperand) cc.getLeft()).getPropertyKey());
    }

    @Test
    void testAnonymousNode() {
        MatchOperation op = parse("MATCH (:Person) RETURN count(*)");
        assertEquals(1, op.getPattern().nodeCount());
        assertTrue(op.getPattern().getNodes().get(0).getVariableName().startsWith("_anon_"));
    }

    @Test
    void testNodeWithProperty() {
        MatchOperation op = parse("MATCH (p:Person {name: \"Alice\"}) RETURN p");
        assertEquals(1, countOf(op, PropertyCondition.class));
        PropertyCondition prop = (PropertyCondition) op.getConditions().stream()
                .filter(PropertyCondition.class::isInstance).findFirst()
                .orElseThrow(() -> new AssertionError("no PropertyCondition found"));
        assertEquals("p", prop.getVariableName());
        assertEquals("name", prop.getPropertyKey());
        assertEquals(new LiteralOperand("Alice"), prop.getValue());
    }

    @Test
    void testPropertyConstantArithmeticFolded() {
        // A property value is an operand too, so a constant expression folds at parse time.
        MatchOperation op = parse("MATCH (p {age: 25 + 5}) RETURN p");
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand(30L).equals(((PropertyCondition) c).getValue())));
    }

    @Test
    void testPropertyFunctionValueKeptAsRawOperand() {
        // A function-call value is kept whole as a RawOperand, not misread as an inner literal.
        PropertyCondition prop = (PropertyCondition) parse("MATCH (p {at: time(\"11:11\")}) RETURN p")
                .getConditions().stream().filter(PropertyCondition.class::isInstance).findFirst().orElseThrow(AssertionError::new);
        assertInstanceOf(RawOperand.class, prop.getValue());
        assertEquals("time(\"11:11\")", ((RawOperand) prop.getValue()).getText());
    }

    @Test
    void testPropertyParameterValueKeptAsRawOperand() {
        PropertyCondition prop = (PropertyCondition) parse("MATCH (p {age: $minAge}) RETURN p")
                .getConditions().stream().filter(PropertyCondition.class::isInstance).findFirst().orElseThrow(AssertionError::new);
        assertInstanceOf(RawOperand.class, prop.getValue());
    }

    @Test
    void testNodeWithMultipleProperties() {
        MatchOperation op = parse("MATCH (p:Person {name: \"Alice\", age: 30}) RETURN p");
        assertEquals(2, countOf(op, PropertyCondition.class));
    }

    @Test
    void testPropertyWithSingleQuotes() {
        MatchOperation op = parse("MATCH (p:Person {name: 'Alice'}) RETURN p");
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand("Alice").equals(((PropertyCondition) c).getValue())));
    }

    @Test
    void testNumericPropertyValues() {
        MatchOperation op = parse("MATCH (p:Person {age: 30, salary: 50000.50}) RETURN p");
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand(30L).equals(((PropertyCondition) c).getValue())));
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand(50000.50).equals(((PropertyCondition) c).getValue())));
    }

    @Test
    void testBooleanPropertyValues() {
        MatchOperation op = parse("MATCH (p:Person {active: true, deleted: false}) RETURN p");
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand(true).equals(((PropertyCondition) c).getValue())));
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand(false).equals(((PropertyCondition) c).getValue())));
    }

    @Test
    void testSimpleRelationshipWithBrackets() {
        MatchOperation op = parse("MATCH (a)-[r]->(b) RETURN a, b");
        assertEquals(2, op.getPattern().nodeCount());
        assertEquals(1, op.getPattern().edgeCount());
        PatternEdge edge = op.getPattern().getEdges().get(0);
        assertEquals("a", edge.getSourceVariable());
        assertEquals("b", edge.getTargetVariable());
        assertTrue(edge.isDirected());
    }

    @Test
    void testEmptyRelationshipDirected() {
        MatchOperation op = parse("MATCH (a)-->(b) RETURN a, b");
        assertEquals(1, op.getPattern().edgeCount());
        assertTrue(op.getPattern().getEdges().get(0).isDirected());
    }

    @Test
    void testEmptyRelationshipUndirected() {
        MatchOperation op = parse("MATCH (a)--(b) RETURN a, b");
        assertFalse(op.getPattern().getEdges().get(0).isDirected());
    }

    @Test
    void testLeftArrowRelationship() {
        MatchOperation op = parse("MATCH (a)<-[r]-(b) RETURN a, b");
        PatternEdge edge = op.getPattern().getEdges().get(0);
        assertEquals("b", edge.getSourceVariable());
        assertEquals("a", edge.getTargetVariable());
        assertTrue(edge.isDirected());
    }

    @Test
    void testRelationshipWithType() {
        MatchOperation op = parse("MATCH (a)-[r:KNOWS]->(b) RETURN a, b");
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof TypeCondition && "KNOWS".equals(((TypeCondition) c).getType())));
    }

    @Test
    void testMultipleRelationshipTypes() {
        // [:ACTED_IN|DIRECTED] is an OR over the two types, not an (impossible) AND.
        MatchOperation op = parse("MATCH (:Movie)<-[:ACTED_IN|DIRECTED]-(person:Person) RETURN person");
        assertEquals(2, op.getPattern().nodeCount());
        assertEquals(1, op.getPattern().edgeCount());
        OrCondition or = (OrCondition) op.getConditions().stream()
                .filter(OrCondition.class::isInstance).findFirst()
                .orElseThrow(() -> new AssertionError("no OrCondition for relationship types"));
        assertEquals(2, or.getConditions().size());
        assertTrue(or.getConditions().stream().allMatch(TypeCondition.class::isInstance));
    }

    @Test
    void testRelationshipTypeNegation() {
        MatchOperation op = parse("MATCH (a)-[r:!KNOWS]->(b) RETURN a, b");
        NotCondition not = (NotCondition) op.getConditions().stream()
                .filter(NotCondition.class::isInstance).findFirst()
                .orElseThrow(() -> new AssertionError("no NotCondition for relationship type"));
        assertEquals("KNOWS", ((TypeCondition) not.getCondition()).getType());
    }

    @Test
    void testRelationshipWithProperties() {
        MatchOperation op = parse("MATCH (a)-[r:ACTED_IN {role: 'Harry Potter'}]-(b) RETURN a, b");
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof PropertyCondition && new LiteralOperand("Harry Potter").equals(((PropertyCondition) c).getValue())));
    }

    @Test
    void testVariableLengthAny() {
        PatternEdge edge = parse("MATCH (a)-[*]->(b) RETURN a, b").getPattern().getEdges().get(0);
        assertTrue(edge.isVariableLength());
    }

    @Test
    void testVariableLengthRange() {
        PatternEdge edge = parse("MATCH (a)-[*1..3]->(b) RETURN a, b").getPattern().getEdges().get(0);
        assertTrue(edge.isVariableLength());
        assertEquals(1, edge.getMinLength().intValue());
        assertEquals(3, edge.getMaxLength().intValue());
    }

    @Test
    void testVariableLengthMinOnly() {
        PatternEdge edge = parse("MATCH (a)-[*2..]->(b) RETURN a, b").getPattern().getEdges().get(0);
        assertEquals(2, edge.getMinLength().intValue());
        assertNull(edge.getMaxLength());
    }

    @Test
    void testVariableLengthMaxOnly() {
        PatternEdge edge = parse("MATCH (a)-[*..5]->(b) RETURN a, b").getPattern().getEdges().get(0);
        assertNull(edge.getMinLength());
        assertEquals(5, edge.getMaxLength().intValue());
    }

    @Test
    void testVariableLengthExact() {
        PatternEdge edge = parse("MATCH (a)-[*3]->(b) RETURN a, b").getPattern().getEdges().get(0);
        assertEquals(3, edge.getMinLength().intValue());
        assertEquals(3, edge.getMaxLength().intValue());
    }

    @Test
    void testVariableLengthWithType() {
        MatchOperation op = parse("MATCH (a)-[:KNOWS*1..3]->(b) RETURN a, b");
        PatternEdge edge = op.getPattern().getEdges().get(0);
        assertEquals(1, edge.getMinLength().intValue());
        assertEquals(3, edge.getMaxLength().intValue());
        assertTrue(op.getConditions().stream().anyMatch(c ->
                c instanceof TypeCondition && "KNOWS".equals(((TypeCondition) c).getType())));
    }

    @Test
    void testChainedPattern() {
        MatchOperation op = parse("MATCH (a)-[r1]->(b)-[r2]->(c) RETURN a, b, c");
        assertEquals(3, op.getPattern().nodeCount());
        assertEquals(2, op.getPattern().edgeCount());
    }

    @Test
    void testMultiplePatternsWithComma() {
        MatchOperation op = parse("MATCH (a:Person), (b:Movie) RETURN a, b");
        assertEquals(2, op.getPattern().nodeCount());
        assertEquals(0, op.getPattern().edgeCount());
        assertEquals(2, countOf(op, LabelCondition.class));
    }

    @Test
    void testMultipleRelationshipPatterns() {
        MatchOperation op = parse("MATCH (a)-[:KNOWS]->(b), (a)-[:WORKS_WITH]->(c) RETURN b, c");
        assertEquals(3, op.getPattern().nodeCount());
        assertEquals(2, op.getPattern().edgeCount());
    }

    @Test
    void testPathAssignment() {
        MatchOperation op = parse("MATCH path = (a)-[:KNOWS]->(b) RETURN path");
        assertEquals(2, op.getPattern().nodeCount());
        assertEquals(1, op.getPattern().edgeCount());
        assertEquals("path", op.getPathVariable());
        assertEquals(1, op.getPathVariables().size());
        assertEquals("path", op.getPathVariables().get(0));
        assertFalse(op.isOptional());
    }

    @Test
    void testMultiplePathAssignmentsKept() {
        // MATCH p = ..., q = ... keeps both path variables, in source order.
        MatchOperation op = parse("MATCH p = (a)-[:KNOWS]->(b), q = (c)-[:LIKES]->(d) RETURN p, q");
        assertEquals(2, op.getPathVariables().size());
        assertEquals("p", op.getPathVariables().get(0));
        assertEquals("q", op.getPathVariables().get(1));
        assertEquals("p", op.getPathVariable());
    }

    @Test
    void testOptionalMatchFlag() {
        assertTrue(parse("OPTIONAL MATCH (p)-[:KNOWS]->(f) RETURN f").isOptional());
        assertFalse(parse("MATCH (p)-[:KNOWS]->(f) RETURN f").isOptional());
    }

    @Test
    void testQuantifiedPathRange() {
        MatchOperation op = parse("MATCH ((a)-[:KNOWS]->(b)){1,3} RETURN a");
        assertEquals(0, op.getPattern().nodeCount());
        assertEquals(0, op.getPattern().edgeCount());
        assertEquals(1, op.getPattern().quantifiedPathCount());

        QuantifiedPathPattern qpp = op.getPattern().getQuantifiedPaths().get(0);
        assertEquals(1, qpp.getMin());
        assertEquals(3, qpp.getMax().intValue());
        assertEquals(2, qpp.getSubPattern().nodeCount());
        assertEquals(1, qpp.getSubPattern().edgeCount());
        // The :KNOWS type is scoped to the QPP, not flattened into the outer operation's list.
        assertEquals(0, countOf(op, TypeCondition.class));
        assertEquals(1, qpp.getConditions().stream().filter(TypeCondition.class::isInstance).count());
    }

    @Test
    void testQuantifiedPathConditionsScopedToSubPattern() {
        // Labels, types and the inline WHERE of a QPP belong to the QPP, not the global condition list.
        MatchOperation op = parse("MATCH ((a:Person)-[:KNOWS]->(b) WHERE a.age > 30){1,3} RETURN a");
        assertEquals(0, op.getConditions().size());
        QuantifiedPathPattern qpp = op.getPattern().getQuantifiedPaths().get(0);
        assertEquals(1, qpp.getConditions().stream().filter(LabelCondition.class::isInstance).count());
        assertEquals(1, qpp.getConditions().stream().filter(TypeCondition.class::isInstance).count());
        assertEquals(1, qpp.getConditions().stream().filter(ComparisonCondition.class::isInstance).count());
    }

    @Test
    void testQuantifiedPathOneOrMore() {
        QuantifiedPathPattern qpp = parse("MATCH ((a)-[:KNOWS]->(b))+ RETURN a")
                .getPattern().getQuantifiedPaths().get(0);
        assertEquals(1, qpp.getMin());
        assertNull(qpp.getMax());
        assertTrue(qpp.isUnboundedMax());
    }

    @Test
    void testQuantifiedPathZeroOrMore() {
        QuantifiedPathPattern qpp = parse("MATCH ((a)-[:KNOWS]->(b))* RETURN a")
                .getPattern().getQuantifiedPaths().get(0);
        assertEquals(0, qpp.getMin());
        assertNull(qpp.getMax());
    }

    @Test
    void testQuantifiedPathExact() {
        QuantifiedPathPattern qpp = parse("MATCH ((a)-[:KNOWS]->(b)){2} RETURN a")
                .getPattern().getQuantifiedPaths().get(0);
        assertEquals(2, qpp.getMin());
        assertEquals(2, qpp.getMax().intValue());
    }

    @Test
    void testQuantifiedPathMultiEdgeSubPath() {
        QuantifiedPathPattern qpp = parse("MATCH ((a)-[:R]->(b)-[:S]->(c)){1,2} RETURN a")
                .getPattern().getQuantifiedPaths().get(0);
        assertEquals(3, qpp.getSubPattern().nodeCount());
        assertEquals(2, qpp.getSubPattern().edgeCount());
    }

    @Test
    void testQuantifiedPathMixedWithPlainElements() {
        MatchOperation op = parse(
                "MATCH (start)-[:R]->(a) ((a)-[:KNOWS]->(b)){1,3} (b)-[:S]->(end) RETURN start");
        assertEquals(4, op.getPattern().nodeCount());   // start, a, b, end
        assertEquals(2, op.getPattern().edgeCount());   // the two plain edges
        assertEquals(1, op.getPattern().quantifiedPathCount());

        QuantifiedPathPattern qpp = op.getPattern().getQuantifiedPaths().get(0);
        assertEquals("a", qpp.getEntryVariable());
        assertEquals("b", qpp.getExitVariable());
    }

    @Test
    void testQuantifiedPathAtPatternStart() {
        MatchOperation op = parse("MATCH ((a)-[:KNOWS]->(b))+ (c) RETURN a");
        QuantifiedPathPattern qpp = op.getPattern().getQuantifiedPaths().get(0);
        assertNull(qpp.getEntryVariable());
        assertEquals("c", qpp.getExitVariable());
    }

    @Test
    void testQuantifiedPathAtPatternEnd() {
        MatchOperation op = parse("MATCH (c) ((a)-[:KNOWS]->(b))+ RETURN a");
        QuantifiedPathPattern qpp = op.getPattern().getQuantifiedPaths().get(0);
        assertEquals("c", qpp.getEntryVariable());
        assertNull(qpp.getExitVariable());
    }

    @Test
    void testQuantifiedPathAsWholePatternHasNoBoundary() {
        QuantifiedPathPattern qpp = parse("MATCH ((a)-[:KNOWS]->(b))+ RETURN a")
                .getPattern().getQuantifiedPaths().get(0);
        assertNull(qpp.getEntryVariable());
        assertNull(qpp.getExitVariable());
    }

    @Test
    void testAdjacentQuantifiedPathsShareSynthesizedBoundary() {
        MatchOperation op = parse("MATCH ((a)-[:LIKES]->(b))+ ((c)-[:KNOWS]->(d))+ RETURN a, d");
        assertEquals(2, op.getPattern().quantifiedPathCount());

        QuantifiedPathPattern qpp1 = op.getPattern().getQuantifiedPaths().get(0);
        QuantifiedPathPattern qpp2 = op.getPattern().getQuantifiedPaths().get(1);
        assertNull(qpp1.getEntryVariable());
        assertNotNull(qpp1.getExitVariable());
        assertEquals(qpp1.getExitVariable(), qpp2.getEntryVariable());
        assertNull(qpp2.getExitVariable());
    }

    @Test
    void testThreeAdjacentQuantifiedPathsChainBoundaries() {
        MatchOperation op = parse("MATCH ((a)-[:R]->(b))+ ((c)-[:S]->(d))+ ((e)-[:T]->(f))+ RETURN a");
        QuantifiedPathPattern qpp1 = op.getPattern().getQuantifiedPaths().get(0);
        QuantifiedPathPattern qpp2 = op.getPattern().getQuantifiedPaths().get(1);
        QuantifiedPathPattern qpp3 = op.getPattern().getQuantifiedPaths().get(2);

        assertNull(qpp1.getEntryVariable());
        assertEquals(qpp1.getExitVariable(), qpp2.getEntryVariable());
        assertEquals(qpp2.getExitVariable(), qpp3.getEntryVariable());
        assertNull(qpp3.getExitVariable());

        assertNotEquals(qpp1.getExitVariable(), qpp2.getExitVariable());
    }

    @Test
    void testQuantifiedPathNested() {
        MatchOperation op = parse("MATCH (((a)-[:R]->(b)){1,2}){1,3} RETURN a");
        assertEquals(1, op.getPattern().quantifiedPathCount());

        QuantifiedPathPattern outer = op.getPattern().getQuantifiedPaths().get(0);
        assertEquals(1, outer.getMin());
        assertEquals(3, outer.getMax().intValue());
        assertEquals(1, outer.getSubPattern().quantifiedPathCount());
        assertNull(outer.getEntryVariable());
        assertNull(outer.getExitVariable());

        QuantifiedPathPattern inner = outer.getSubPattern().getQuantifiedPaths().get(0);
        assertEquals(1, inner.getMin());
        assertEquals(2, inner.getMax().intValue());
        assertEquals(2, inner.getSubPattern().nodeCount());
        assertEquals(1, inner.getSubPattern().edgeCount());

        assertNull(inner.getEntryVariable());
        assertNull(inner.getExitVariable());
    }

    @Test
    void testWhereEquality() {
        MatchOperation op = parse("MATCH (p:Person) WHERE p.age = 25 RETURN p");
        ComparisonCondition cc = comparison(op);
        PropertyOperand left = (PropertyOperand) cc.getLeft();
        assertEquals("p", left.getVariableName());
        assertEquals("age", left.getPropertyKey());
        assertEquals(ComparisonOperator.EQUALS, cc.getOperator());
        assertEquals(25L, ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testWhereGreaterThan() {
        assertEquals(ComparisonOperator.GREATER_THAN,
                comparison(parse("MATCH (p:Person) WHERE p.age > 25 RETURN p")).getOperator());
    }

    @Test
    void testWhereLessThan() {
        assertEquals(ComparisonOperator.LESS_THAN,
                comparison(parse("MATCH (p:Person) WHERE p.age < 25 RETURN p")).getOperator());
    }

    @Test
    void testWhereNotEquals() {
        assertEquals(ComparisonOperator.NOT_EQUALS,
                comparison(parse("MATCH (p:Person) WHERE p.age <> 25 RETURN p")).getOperator());
    }

    @Test
    void testWhereGreaterThanOrEqual() {
        assertEquals(ComparisonOperator.GREATER_THAN_OR_EQUALS,
                comparison(parse("MATCH (p:Person) WHERE p.age >= 25 RETURN p")).getOperator());
    }

    @Test
    void testWhereLessThanOrEqual() {
        assertEquals(ComparisonOperator.LESS_THAN_OR_EQUALS,
                comparison(parse("MATCH (p:Person) WHERE p.age <= 25 RETURN p")).getOperator());
    }

    @Test
    void testWhereMultipleConditionsAnd() {
        MatchOperation op = parse("MATCH (p) WHERE p.age > 18 AND p.age < 65 RETURN p");
        assertEquals(1, op.getConditions().size());
        AndCondition and = (AndCondition) op.getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().stream().allMatch(ComparisonCondition.class::isInstance));
    }

    @Test
    void testWhereOr() {
        MatchOperation op = parse("MATCH (p) WHERE p.age < 18 OR p.age > 65 RETURN p");
        OrCondition or = (OrCondition) op.getConditions().get(0);
        assertEquals(2, or.getConditions().size());
        assertTrue(or.getConditions().stream().allMatch(ComparisonCondition.class::isInstance));
    }

    @Test
    void testWhereNot() {
        MatchOperation op = parse("MATCH (p) WHERE NOT p.active = true RETURN p");
        NotCondition not = (NotCondition) op.getConditions().get(0);
        assertInstanceOf(ComparisonCondition.class, not.getCondition());
    }

    @Test
    void testWhereDoubleNegationCancels() {
        MatchOperation op = parse("MATCH (p) WHERE NOT NOT p.x = 1 RETURN p");
        assertInstanceOf(ComparisonCondition.class, op.getConditions().get(0));
    }

    @Test
    void testWhereXor() {
        MatchOperation op = parse("MATCH (p) WHERE p.x = 1 XOR p.y = 2 RETURN p");
        XorCondition xor = (XorCondition) op.getConditions().get(0);
        assertEquals(2, xor.getConditions().size());
    }

    @Test
    void testWhereParenthesesGroup() {
        // The parens force (b OR c) to be one branch of the AND.
        MatchOperation op = parse("MATCH (p) WHERE p.a = 1 AND (p.b = 2 OR p.c = 3) RETURN p");
        AndCondition and = (AndCondition) op.getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        assertInstanceOf(ComparisonCondition.class, and.getConditions().get(0));
        OrCondition or = (OrCondition) and.getConditions().get(1);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    void testWhereNotOfGroup() {
        MatchOperation op = parse("MATCH (p) WHERE NOT (p.a = 1 OR p.b = 2) RETURN p");
        NotCondition not = (NotCondition) op.getConditions().get(0);
        assertInstanceOf(OrCondition.class, not.getCondition());
    }

    @Test
    void testWherePrecedenceOrOfAnds() {
        // a AND b OR c  ==  (a AND b) OR c
        MatchOperation op = parse("MATCH (p) WHERE p.a = 1 AND p.b = 2 OR p.c = 3 RETURN p");
        OrCondition or = (OrCondition) op.getConditions().get(0);
        assertEquals(2, or.getConditions().size());
        assertInstanceOf(AndCondition.class, or.getConditions().get(0));
        assertInstanceOf(ComparisonCondition.class, or.getConditions().get(1));
    }

    @Test
    void testWhereStringComparison() {
        ComparisonCondition cc = comparison(parse("MATCH (p:Person) WHERE p.name = \"Alice\" RETURN p"));
        assertEquals("name", ((PropertyOperand) cc.getLeft()).getPropertyKey());
        assertEquals("Alice", ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testWhereStartsWith() {
        assertEquals(ComparisonOperator.STARTS_WITH,
                comparison(parse("MATCH (p:Person) WHERE p.name STARTS WITH 'A' RETURN p")).getOperator());
    }

    @Test
    void testWhereEndsWith() {
        assertEquals(ComparisonOperator.ENDS_WITH,
                comparison(parse("MATCH (p:Person) WHERE p.name ENDS WITH 'son' RETURN p")).getOperator());
    }

    @Test
    void testWhereContains() {
        assertEquals(ComparisonOperator.CONTAINS,
                comparison(parse("MATCH (p:Person) WHERE p.name CONTAINS 'li' RETURN p")).getOperator());
    }

    @Test
    void testWhereIsNull() {
        assertEquals(ComparisonOperator.IS_NULL,
                comparison(parse("MATCH (p:Person) WHERE p.email IS NULL RETURN p")).getOperator());
    }

    @Test
    void testWhereIsNotNull() {
        assertEquals(ComparisonOperator.IS_NOT_NULL,
                comparison(parse("MATCH (p:Person) WHERE p.email IS NOT NULL RETURN p")).getOperator());
    }

    @Test
    void testComparisonPropertyVsLiteral() {
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.age > 25 RETURN p"));
        PropertyOperand left = (PropertyOperand) cc.getLeft();
        assertEquals("p", left.getVariableName());
        assertEquals("age", left.getPropertyKey());
        assertEquals(25L, ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testComparisonPropertyVsProperty() {
        // Typed operands keep both sides of n.age > m.age as property references.
        ComparisonCondition cc = comparison(parse("MATCH (n)-->(m) WHERE n.age > m.age RETURN n"));
        PropertyOperand left = (PropertyOperand) cc.getLeft();
        PropertyOperand right = (PropertyOperand) cc.getRight();
        assertEquals("n", left.getVariableName());
        assertEquals("age", left.getPropertyKey());
        assertEquals("m", right.getVariableName());
        assertEquals("age", right.getPropertyKey());
    }

    @Test
    void testComparisonInListDecomposed() {
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.age IN [18, 21, 65] RETURN p"));
        assertEquals(ComparisonOperator.IN, cc.getOperator());
        ListOperand list = (ListOperand) cc.getRight();
        assertEquals(3, list.getElements().size());
        assertEquals(18L, ((LiteralOperand) list.getElements().get(0)).getValue());
        assertEquals(65L, ((LiteralOperand) list.getElements().get(2)).getValue());
    }

    @Test
    void testComparisonInListWithPropertyElement() {
        // Elements are parsed recursively, so a property reference inside the list is preserved.
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.age IN [p.min, 40] RETURN p"));
        ListOperand list = (ListOperand) cc.getRight();
        assertEquals("min", ((PropertyOperand) list.getElements().get(0)).getPropertyKey());
        assertEquals(40L, ((LiteralOperand) list.getElements().get(1)).getValue());
    }

    @Test
    void testComparisonConstantArithmeticFolded() {
        // A fully-literal RHS is evaluated at parse time, so the calculator sees a single value.
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.age > 25 + 5 RETURN p"));
        assertInstanceOf(PropertyOperand.class, cc.getLeft());
        assertEquals(30L, ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testComparisonConstantArithmeticPrecedenceFolded() {
        // Precedence and a double operand: 2 + 3 * 4 = 14, kept as a double once a double is involved.
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.age > 2 + 3 * 4.0 RETURN p"));
        assertEquals(14.0, ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testConstantArithmeticFoldsInLongDomainWithoutPrecisionLoss() {
        // 2^53 + 1 is exact as a long but not as a double; folding in the long domain keeps it.
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.age = 9007199254740992 + 1 RETURN p"));
        assertEquals(9007199254740993L, ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testConstantArithmeticOverflowLeftUnfolded() {
        // Long overflow would yield a wrong wrapped constant, so the node is kept structural instead.
        ComparisonCondition cc = comparison(
                parse("MATCH (p) WHERE p.age > 9223372036854775807 + 1 RETURN p"));
        assertInstanceOf(ArithmeticOperand.class, cc.getRight());
    }

    @Test
    void testComparisonNonConstantArithmeticKeepsPropertyRef() {
        // A graph-dependent RHS stays a structured tree, with the inner property reference preserved.
        ComparisonCondition cc = comparison(parse("MATCH (a)-[]->(p) WHERE a.age > p.age + 5 RETURN a"));
        ArithmeticOperand rhs = (ArithmeticOperand) cc.getRight();
        assertEquals(ArithmeticOperator.PLUS, rhs.getOperator());
        assertEquals("age", ((PropertyOperand) rhs.getLeft()).getPropertyKey());
        assertEquals(5L, ((LiteralOperand) rhs.getRight()).getValue());
    }

    @Test
    void testChainedComparisonExpandsToAnd() {
        // a < b < c  ≡  (a < b) AND (b < c) — Cypher chains comparisons, no operand is dropped.
        AndCondition and = (AndCondition) parse("MATCH (p) WHERE 18 < p.age < 65 RETURN p")
                .getConditions().get(0);
        assertEquals(2, and.getConditions().size());
        ComparisonCondition first = (ComparisonCondition) and.getConditions().get(0);
        ComparisonCondition second = (ComparisonCondition) and.getConditions().get(1);
        assertEquals(ComparisonOperator.LESS_THAN, first.getOperator());
        assertEquals(18L, ((LiteralOperand) first.getLeft()).getValue());
        assertEquals("age", ((PropertyOperand) first.getRight()).getPropertyKey());
        // The middle operand is repeated as the left side of the second link.
        assertEquals("age", ((PropertyOperand) second.getLeft()).getPropertyKey());
        assertEquals(65L, ((LiteralOperand) second.getRight()).getValue());
    }

    @Test
    void testChainedComparisonMixedOperatorsKeptInOrder() {
        // A mixed chain keeps each operator: a <= b < c.
        AndCondition and = (AndCondition) parse("MATCH (p) WHERE 18 <= p.age < 65 RETURN p")
                .getConditions().get(0);
        assertEquals(ComparisonOperator.LESS_THAN_OR_EQUALS,
                ((ComparisonCondition) and.getConditions().get(0)).getOperator());
        assertEquals(ComparisonOperator.LESS_THAN,
                ((ComparisonCondition) and.getConditions().get(1)).getOperator());
    }

    @Test
    void testComparisonNonNumericConcatKeptRaw() {
        // String concatenation (||) is not numeric arithmetic, so it stays a RawOperand.
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.name = \"a\" || \"b\" RETURN p"));
        assertInstanceOf(RawOperand.class, cc.getRight());
    }

    @Test
    void testComparisonStringLiteralOperand() {
        ComparisonCondition cc = comparison(parse("MATCH (p) WHERE p.name = \"Alice\" RETURN p"));
        assertEquals("Alice", ((LiteralOperand) cc.getRight()).getValue());
    }

    @Test
    void testComplexExample() {
        MatchOperation op = parse(
                "MATCH (p:Person {name: \"Alice\"})-[:KNOWS]->(f:Person) WHERE f.age > 25 RETURN f");
        assertEquals(2, op.getPattern().nodeCount());
        assertEquals(1, op.getPattern().edgeCount());
        assertEquals(2, countOf(op, LabelCondition.class));
        assertEquals(1, countOf(op, PropertyCondition.class));
        assertEquals(1, countOf(op, TypeCondition.class));
        assertEquals(1, countOf(op, ComparisonCondition.class));
    }

    @Test
    void testDocExampleMultipleRelChain() {
        MatchOperation op = parse(
                "MATCH (:Person)-[:ACTED_IN]->(movie:Movie)<-[:DIRECTED]-(director) RETURN movie, director");
        assertEquals(3, op.getPattern().nodeCount());
        assertEquals(2, op.getPattern().edgeCount());
    }

    @Test
    void testMatchCaseInsensitive() {
        assertEquals(1, parse("match (n:Person) return n").getPattern().nodeCount());
    }

    @Test
    void testNullQueryFails() {
        assertFails(null);
    }

    @Test
    void testEmptyQueryFails() {
        assertFails("");
    }

    @Test
    void testWhitespaceOnlyQueryFails() {
        assertFails("   \t\n  ");
    }

    @Test
    void testNonMatchQueryFails() {
        assertFails("CREATE (n:Person) RETURN n");
    }

    @Test
    void testSyntaxErrorFails() {
        assertFails("MATCH (n:Person RETURN n"); // missing closing paren
    }

}
