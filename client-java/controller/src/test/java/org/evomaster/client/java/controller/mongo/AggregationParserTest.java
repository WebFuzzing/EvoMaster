package org.evomaster.client.java.controller.mongo;

import org.bson.Document;
import org.evomaster.client.java.controller.mongo.operations.EmptyOperation;
import org.evomaster.client.java.controller.mongo.operations.GreaterThanEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperationWithFieldPath;
import org.evomaster.client.java.controller.mongo.operations.aggregations.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationParserTest {

    private final AggregationParser parser = new AggregationParser();

    private static Document doc(Object... fieldsAndValues) {
        Document document = new Document();
        for (int i = 0; i < fieldsAndValues.length; i += 2) {
            document.append((String) fieldsAndValues[i], fieldsAndValues[i + 1]);
        }
        return document;
    }

    private static Document stage(String name, Object specification) {
        return new Document(name, specification);
    }

    private AggregationStage parseStage(Document stage) {
        List<AggregationStage> stages = parser.parse(Collections.singletonList(stage)).getStages();
        assertEquals(1, stages.size());
        return stages.get(0);
    }

    private <T extends AggregationStage> T parseStage(Document stage, Class<T> expected) {
        AggregationStage parsed = parseStage(stage);
        assertEquals(expected, parsed.getClass());
        return expected.cast(parsed);
    }

    private AggregationExpression parseComputed(Object expression) {
        ProjectStage project = parseStage(stage("$project", doc("x", expression)), ProjectStage.class);
        assertEquals(ProjectFieldType.COMPUTED, project.getFields().get(0).getType());
        return project.getFields().get(0).getExpression();
    }

    // ------------------------------------------------------------------ pipeline

    @Test
    public void testEmptyPipeline() {
        assertTrue(parser.parse(Collections.emptyList()).getStages().isEmpty());
    }

    @Test
    public void testStagesKeepTheirOrder() {
        AggregationPipeline pipeline = parser.parse(Arrays.asList(
                stage("$match", doc("a", 1)),
                stage("$sort", doc("a", 1)),
                stage("$limit", 5)));

        assertEquals(Arrays.asList("$match", "$sort", "$limit"),
                Arrays.asList(pipeline.getStages().get(0).getStageName(),
                        pipeline.getStages().get(1).getStageName(),
                        pipeline.getStages().get(2).getStageName()));
    }

    @Test
    public void testStageMustBeADocumentWithOneField() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList("$match")));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                new Document("$match", doc("a", 1)).append("$limit", 1))));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(new Document())));
    }

    @Test
    public void testNullPipelineIsRejected() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void testUnknownStageIsUnsupported() {
        UnsupportedStage unsupported = parseStage(stage("$unwind", "$tags"), UnsupportedStage.class);
        assertEquals("$unwind", unsupported.getStageName());
        assertFalse(unsupported.getReason().isEmpty());
    }

    // ------------------------------------------------------------------ $match

    @Test
    public void testMatchHasTheParsedQuery() {
        MatchStage match = parseStage(stage("$match", doc("age", doc("$gte", 30))), MatchStage.class);

        assertTrue(match.getFilter() instanceof GreaterThanEqualsOperation);
        assertEquals("age", ((QueryOperationWithFieldPath) match.getFilter()).getFieldPath());
    }

    @Test
    public void testMatchOfAnEmptyDocumentIsTheEmptyQuery() {
        MatchStage match = parseStage(stage("$match", new Document()), MatchStage.class);
        assertTrue(match.getFilter() instanceof EmptyOperation);
    }

    @Test
    public void testMatchMustBeADocument() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(stage("$match", 42))));
    }

    // ------------------------------------------------------------------ $group

    @Test
    public void testGroupHasTheKeyAndTheAccumulators() {
        GroupStage group = parseStage(stage("$group", doc(
                "_id", "$city",
                "total", doc("$sum", "$amount"),
                "n", doc("$count", new Document()))), GroupStage.class);

        assertTrue(group.getId() instanceof FieldPathExpression);
        assertEquals("city", ((FieldPathExpression) group.getId()).getFieldPath());

        assertEquals(2, group.getAccumulators().size());
        GroupAccumulator total = group.getAccumulators().get(0);
        assertEquals("total", total.getFieldName());
        assertEquals(AccumulatorOperator.SUM, total.getOperator());
        assertTrue(total.isSupported());
        assertEquals("amount", ((FieldPathExpression) total.getArgument()).getFieldPath());

        GroupAccumulator count = group.getAccumulators().get(1);
        assertEquals(AccumulatorOperator.COUNT, count.getOperator());
        assertNull(count.getArgument());
    }

    @Test
    public void testGroupParsesEveryAccumulator() {
        GroupStage group = parseStage(stage("$group", doc("_id", null,
                "a", doc("$sum", 1), "b", doc("$avg", "$x"), "c", doc("$min", "$x"), "d", doc("$max", "$x"),
                "e", doc("$first", "$x"), "f", doc("$last", "$x"), "g", doc("$push", "$x"), "h", doc("$addToSet", "$x"))),
                GroupStage.class);

        assertEquals(Arrays.asList(AccumulatorOperator.SUM, AccumulatorOperator.AVG, AccumulatorOperator.MIN,
                        AccumulatorOperator.MAX, AccumulatorOperator.FIRST, AccumulatorOperator.LAST,
                        AccumulatorOperator.PUSH, AccumulatorOperator.ADD_TO_SET),
                Arrays.asList(group.getAccumulators().get(0).getOperator(), group.getAccumulators().get(1).getOperator(),
                        group.getAccumulators().get(2).getOperator(), group.getAccumulators().get(3).getOperator(),
                        group.getAccumulators().get(4).getOperator(), group.getAccumulators().get(5).getOperator(),
                        group.getAccumulators().get(6).getOperator(), group.getAccumulators().get(7).getOperator()));
    }

    @Test
    public void testGroupKeyCanBeADocumentOrNull() {
        GroupStage compound = parseStage(stage("$group", doc("_id", doc("city", "$city", "year", "$year"))), GroupStage.class);
        assertTrue(compound.getId() instanceof DocumentExpression);
        assertEquals(2, ((DocumentExpression) compound.getId()).getFields().size());

        GroupStage all = parseStage(stage("$group", doc("_id", null)), GroupStage.class);
        assertTrue(all.getId() instanceof LiteralExpression);
        assertNull(((LiteralExpression) all.getId()).getValue());
    }

    @Test
    public void testUnsupportedAccumulatorIsKept() {
        GroupStage group = parseStage(stage("$group", doc("_id", null, "s", doc("$stdDevPop", "$x"))), GroupStage.class);

        GroupAccumulator accumulator = group.getAccumulators().get(0);
        assertFalse(accumulator.isSupported());
        assertNull(accumulator.getOperator());
        assertEquals("$stdDevPop", accumulator.getOperatorName());
    }

    @Test
    public void testInvalidGroups() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$group", doc("n", doc("$sum", 1))))));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$group", doc("_id", null, "n", 1)))));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$group", doc("_id", null, "n", doc("$sum", 1).append("$avg", 1))))));
    }

    // ------------------------------------------------------------------ $project

    @Test
    public void testProjectInclusionExclusionAndComputedFields() {
        ProjectStage inclusion = parseStage(stage("$project", doc("a", 1, "b", true)), ProjectStage.class);
        assertEquals(ProjectFieldType.INCLUDE, inclusion.getFields().get(0).getType());
        assertEquals(ProjectFieldType.INCLUDE, inclusion.getFields().get(1).getType());
        assertFalse(inclusion.isExclusion());
        assertTrue(inclusion.isIdIncluded());

        ProjectStage exclusion = parseStage(stage("$project", doc("a", 0, "b", false)), ProjectStage.class);
        assertEquals(ProjectFieldType.EXCLUDE, exclusion.getFields().get(0).getType());
        assertTrue(exclusion.isExclusion());

        ProjectStage computed = parseStage(stage("$project", doc("total", "$a")), ProjectStage.class);
        assertEquals(ProjectFieldType.COMPUTED, computed.getFields().get(0).getType());
        assertNotNull(computed.getFields().get(0).getExpression());
    }

    @Test
    public void testProjectIdDoesNotCountAsExclusion() {
        ProjectStage project = parseStage(stage("$project", doc("_id", 0, "a", 1)), ProjectStage.class);

        assertFalse(project.isExclusion());
        assertFalse(project.isIdIncluded());
    }

    @Test
    public void testProjectCannotIncludeAndExclude() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$project", doc("a", 1, "b", 0)))));
        // in any order
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$project", doc("b", 0, "a", 1)))));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$project", doc("b", 0, "total", "$a")))));
    }

    // ------------------------------------------------------------------ $sort and $limit

    @Test
    public void testSortFieldsAndDirections() {
        SortStage sort = parseStage(stage("$sort", doc("a", 1, "b", -1)), SortStage.class);

        assertEquals("a", sort.getFields().get(0).getFieldPath());
        assertTrue(sort.getFields().get(0).isAscending());
        assertEquals("b", sort.getFields().get(1).getFieldPath());
        assertFalse(sort.getFields().get(1).isAscending());
    }

    @Test
    public void testSortByMetaIsUnsupported() {
        UnsupportedStage unsupported = parseStage(stage("$sort", doc("score", doc("$meta", "textScore"))), UnsupportedStage.class);
        assertEquals("$sort", unsupported.getStageName());
    }

    @Test
    public void testSortNeedsFields() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(stage("$sort", new Document()))));
    }

    @Test
    public void testLimit() {
        assertEquals(7, parseStage(stage("$limit", 7), LimitStage.class).getLimit());
        assertEquals(7, parseStage(stage("$limit", 7L), LimitStage.class).getLimit());
    }

    @Test
    public void testLimitMustBeAnInteger() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(stage("$limit", "many"))));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(stage("$limit", 1.5d))));
    }

    // ------------------------------------------------------------------ $lookup and $unionWith

    @Test
    public void testLookup() {
        LookupStage lookup = parseStage(stage("$lookup", doc("from", "customers", "localField", "customerId",
                "foreignField", "_id", "as", "customer")), LookupStage.class);

        assertEquals("customers", lookup.getFrom());
        assertEquals("customerId", lookup.getLocalField());
        assertEquals("_id", lookup.getForeignField());
        assertEquals("customer", lookup.getAs());
    }

    @Test
    public void testLookupWithPipelineIsUnsupported() {
        parseStage(stage("$lookup", doc("from", "customers", "let", doc("id", "$customerId"),
                "pipeline", Collections.emptyList(), "as", "customer")), UnsupportedStage.class);
    }

    @Test
    public void testLookupNeedsAllItsFields() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$lookup", doc("from", "customers")))));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$lookup", doc("from", "customers", "localField", 1, "foreignField", "_id", "as", "c")))));
    }

    @Test
    public void testUnionWithACollection() {
        UnionWithStage union = parseStage(stage("$unionWith", "other"), UnionWithStage.class);

        assertEquals("other", union.getCollection());
        assertTrue(union.getPipeline().getStages().isEmpty());
    }

    @Test
    public void testUnionWithPipelineIsParsedToo() {
        UnionWithStage union = parseStage(stage("$unionWith", doc("coll", "other",
                "pipeline", Arrays.asList(stage("$match", doc("a", 1)), stage("$limit", 2)))), UnionWithStage.class);

        assertEquals("other", union.getCollection());
        assertEquals(2, union.getPipeline().getStages().size());
        assertTrue(union.getPipeline().getStages().get(0) instanceof MatchStage);
        assertTrue(union.getPipeline().getStages().get(1) instanceof LimitStage);
    }

    @Test
    public void testUnionWithNeedsACollection() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(Collections.singletonList(
                stage("$unionWith", doc("pipeline", Collections.emptyList())))));
    }

    // ------------------------------------------------------------------ expressions

    @Test
    public void testLiterals() {
        assertEquals(5, ((LiteralExpression) parseComputed(5)).getValue());
        assertEquals("text", ((LiteralExpression) parseComputed("text")).getValue());
        // true is not a literal in a $project, but the inclusion of the field
        assertEquals(ProjectFieldType.INCLUDE, ((ProjectStage) parseStage(stage("$project", doc("x", true)))).getFields().get(0).getType());
        assertNull(((LiteralExpression) parseComputed(null)).getValue());
    }

    @Test
    public void testLiteralOperatorKeepsDollarSigns() {
        assertEquals("$notAField", ((LiteralExpression) parseComputed(doc("$literal", "$notAField"))).getValue());
    }

    @Test
    public void testFieldPaths() {
        assertEquals("a.b.c", ((FieldPathExpression) parseComputed("$a.b.c")).getFieldPath());
    }

    @Test
    public void testRootVariables() {
        assertTrue(parseComputed("$$ROOT") instanceof RootVariableExpression);
        assertTrue(parseComputed("$$CURRENT") instanceof RootVariableExpression);
    }

    @Test
    public void testOtherVariablesAreUnsupported() {
        assertTrue(parseComputed("$$NOW") instanceof UnsupportedExpression);
    }

    @Test
    public void testArraysAndDocuments() {
        ArrayExpression array = (ArrayExpression) parseComputed(Arrays.asList("$a", 1));
        assertEquals(2, array.getElements().size());
        assertTrue(array.getElements().get(0) instanceof FieldPathExpression);
        assertTrue(array.getElements().get(1) instanceof LiteralExpression);

        DocumentExpression document = (DocumentExpression) parseComputed(doc("x", "$a", "y", 2));
        assertEquals(Arrays.asList("x", "y"), Arrays.asList(document.getFields().keySet().toArray()));
        assertTrue(document.getFields().get("x") instanceof FieldPathExpression);
    }

    @Test
    public void testOperators() {
        OperatorExpression add = (OperatorExpression) parseComputed(doc("$add", Arrays.asList("$a", 2)));
        assertEquals(ExpressionOperator.ADD, add.getOperator());
        assertEquals(2, add.getArguments().size());

        // a single argument that is not in an array
        OperatorExpression size = (OperatorExpression) parseComputed(doc("$size", "$tags"));
        assertEquals(ExpressionOperator.SIZE, size.getOperator());
        assertEquals(1, size.getArguments().size());

        OperatorExpression nested = (OperatorExpression) parseComputed(
                doc("$multiply", Arrays.asList(doc("$add", Arrays.asList(1, 2)), 3)));
        assertTrue(nested.getArguments().get(0) instanceof OperatorExpression);
    }

    @Test
    public void testEveryExpressionOperatorIsParsed() {
        for (ExpressionOperator operator : ExpressionOperator.values()) {
            if (operator == ExpressionOperator.COND) {
                continue;
            }
            OperatorExpression parsed = (OperatorExpression) parseComputed(doc(operator.getOperatorName(), Arrays.asList(1, 2)));
            assertEquals(operator, parsed.getOperator());
        }
    }

    @Test
    public void testCondInBothForms() {
        OperatorExpression array = (OperatorExpression) parseComputed(doc("$cond", Arrays.asList("$a", "yes", "no")));
        OperatorExpression document = (OperatorExpression) parseComputed(doc("$cond", doc("if", "$a", "then", "yes", "else", "no")));

        for (OperatorExpression cond : Arrays.asList(array, document)) {
            assertEquals(ExpressionOperator.COND, cond.getOperator());
            assertEquals(3, cond.getArguments().size());
            assertTrue(cond.getArguments().get(0) instanceof FieldPathExpression);
            assertEquals("yes", ((LiteralExpression) cond.getArguments().get(1)).getValue());
            assertEquals("no", ((LiteralExpression) cond.getArguments().get(2)).getValue());
        }
    }

    @Test
    public void testUnknownOperatorIsUnsupported() {
        UnsupportedExpression unsupported = (UnsupportedExpression) parseComputed(doc("$dateToString", doc("date", "$d")));
        assertTrue(unsupported.getDescription().contains("$dateToString"));
    }

    @Test
    public void testSingleFieldDocumentWithoutDollarIsADocument() {
        assertTrue(parseComputed(doc("name", "$a")) instanceof DocumentExpression);
    }
}
