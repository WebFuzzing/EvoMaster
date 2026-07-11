package org.evomaster.client.java.controller.mongo;

import org.bson.Document;
import org.evomaster.client.java.controller.mongo.operations.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest {

    private final QueryParser parser = new QueryParser();

    @Test
    void testParseEquals() {
        Document query = new Document(
                "age",
                new Document("$eq", 30)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldName());
        assertEquals(30, eq.getValue());
    }

    @Test
    void testParseEqualsNull() {
        Document query = new Document(
                "age",
                new Document("$eq", null)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldName());
        assertEquals(null, eq.getValue());
    }

    @Test
    void testParseNotEquals() {
        Document query = new Document(
                "age",
                new Document("$ne", 30)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NotEqualsOperation);
        NotEqualsOperation<?> ne = (NotEqualsOperation<?>) operation;
        assertEquals("age", ne.getFieldName());
        assertEquals(30, ne.getValue());
    }

    @Test
    void testParseNotEqualsNull() {
        Document query = new Document(
                "age",
                new Document("$ne", null)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NotEqualsOperation);
        NotEqualsOperation<?> ne = (NotEqualsOperation<?>) operation;
        assertEquals("age", ne.getFieldName());
        assertEquals(null, ne.getValue());
    }

    @Test
    void testParseAnd() {
        Document query = new Document(
                "$and",
                Arrays.asList(
                        new Document("age", new Document("$eq", 30)),
                        new Document("name", new Document("$eq", "John"))));

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());
    }

    @Test
    void testParseOr() {
        Document query = new Document(
                "$or",
                Arrays.asList(
                        new Document("age", new Document("$eq", 30)),
                        new Document("name", new Document("$eq", "John"))));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof OrOperation);
        OrOperation or = (OrOperation) operation;
        assertEquals(2, or.getConditions().size());
    }

    @Test
    void testParseIn() {
        Document query = new Document(
                "age",
                new Document("$in", Arrays.asList(20, 30, 40))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof InOperation);
        InOperation in = (InOperation) operation;
        assertEquals("age", in.getFieldName());
        assertEquals(Arrays.asList(20, 30, 40), in.getValues());
    }

    @Test
    void testParseInWithNull() {
        Document query = new Document(
                "age",
                new Document("$in", Arrays.asList(20, 30, null))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof InOperation);
        InOperation in = (InOperation) operation;
        assertEquals("age", in.getFieldName());
        assertEquals(Arrays.asList(20, 30, null), in.getValues());
    }



    @Test
    void testParseExists() {
        Document query = new Document(
                "age",
                new Document("$exists", true)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ExistsOperation);
        ExistsOperation exists = (ExistsOperation) operation;
        assertEquals("age", exists.getFieldName());
        assertTrue(exists.getBoolean());
    }

    @Test
    void testParseNot() {
        Document query = new Document(
                "age",
                new Document("$not", new Document("$eq", 30))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NotOperation);
        NotOperation not = (NotOperation) operation;
        assertEquals("age", not.getFieldName());
        assertTrue(not.getCondition() instanceof EqualsOperation);
    }

    @Test
    void testParseSize() {
        Document query = new Document(
                "tags",
                new Document("$size", 3)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof SizeOperation);
        SizeOperation size = (SizeOperation) operation;
        assertEquals("tags", size.getFieldName());
        assertEquals(3, size.getValue());
    }

    @Test
    void testParseAll() {
        Document query = new Document(
                "tags",
                new Document("$all", Arrays.asList("a", "b"))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AllOperation);
        AllOperation<?> all = (AllOperation<?>) operation;
        assertEquals("tags", all.getFieldName());
        assertEquals(Arrays.asList("a", "b"), all.getValues());
    }

    @Test
    void testParseElemMatch() {
        Document query = new Document(
                "results",
                new Document("$elemMatch", new Document("product", "abc"))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("results", elemMatch.getFieldName());
        // Implicit equals inside elemMatch
        assertTrue(elemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation equals = (EqualsOperation) elemMatch.getCondition();
        assertEquals("product", equals.getFieldName());
        assertEquals("abc", equals.getValue());
    }

    @Test
    void testParseElemMatchWithMultipleConditions() {
        Document query = new Document(
                "results",
                new Document("$elemMatch",
                        new Document("product", "abc").append("quantity", new Document("$gt", 10)))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("results", elemMatch.getFieldName());
        // Implicit and inside elemMatch
        assertTrue(elemMatch.getCondition() instanceof AndOperation);
        AndOperation and = (AndOperation) elemMatch.getCondition();
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof EqualsOperation);
        assertTrue(and.getConditions().get(1) instanceof GreaterThanOperation);

    }

    @Test
    void testParseElemMatchNull() {
        Document query = new Document(
                "results",
                new Document("$elemMatch", new Document("product", null))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("results", elemMatch.getFieldName());
        // Implicit equals inside elemMatch
        assertTrue(elemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation equals = (EqualsOperation) elemMatch.getCondition();
        assertEquals("product", equals.getFieldName());
        assertEquals(null, equals.getValue());
    }

    @Test
    void testParseType() {
        Document query = new Document(
                "name",
                new Document("$type", "STRING")
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof TypeOperation);
        TypeOperation type = (TypeOperation) operation;
        assertEquals("name", type.getFieldName());
        assertNotNull(type.getType());
    }

    @Test
    void testParseTypeWithNumber() {
        Document query = new Document(
                "name",
                new Document("$type", 2) // 2 is string
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof TypeOperation);
        TypeOperation type = (TypeOperation) operation;
        assertEquals("name", type.getFieldName());
        assertNotNull(type.getType());
    }

    @Test
    void testParseGreaterThan() {
        Document query = new Document(
                "age",
                new Document("$gt", 18)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof GreaterThanOperation);
        GreaterThanOperation<?> gt = (GreaterThanOperation<?>) operation;
        assertEquals("age", gt.getFieldName());
        assertEquals(18, gt.getValue());
    }

    @Test
    void testParseLessThanEquals() {
        Document query = new Document(
                "age",
                new Document("$lte", 65)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof LessThanEqualsOperation);
        LessThanEqualsOperation<?> lte = (LessThanEqualsOperation<?>) operation;
        assertEquals("age", lte.getFieldName());
        assertEquals(65, lte.getValue());
    }

    @Test
    void testParseNor() {
        Document query = new Document(
                "$nor",
                Arrays.asList(
                        new Document("a", 1),
                        new Document("b", 2)));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NorOperation);
        NorOperation nor = (NorOperation) operation;
        assertEquals(2, nor.getConditions().size());
    }

    @Test
    void testParseNotIn() {
        Document query = new Document(
                "age",
                new Document("$nin", Arrays.asList(10, 20))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NotInOperation);
        NotInOperation<?> nin = (NotInOperation<?>) operation;
        assertEquals("age", nin.getFieldName());
        assertEquals(Arrays.asList(10, 20), nin.getValues());
    }

    @Test
    void testParseMod() {
        Document query = new Document(
                "age",
                new Document("$mod", Arrays.asList(2L, 0L))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ModOperation);
        ModOperation mod = (ModOperation) operation;
        assertEquals("age", mod.getFieldName());
        assertEquals(2L, mod.getDivisor());
        assertEquals(0L, mod.getRemainder());
    }

    @Test
    void testParseGreaterThanEquals() {
        Document query = new Document(
                "age",
                new Document("$gte", 18)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof GreaterThanEqualsOperation);
        GreaterThanEqualsOperation<?> gte = (GreaterThanEqualsOperation<?>) operation;
        assertEquals("age", gte.getFieldName());
        assertEquals(18, gte.getValue());
    }

    @Test
    void testParseNearSphereLegacy() {
        Document query = new Document(
                "location",
                new Document("$nearSphere", new Document("x", 40.0).append("y", 70.0))
                        .append("$maxDistance", 10.0)
                        .append("$minDistance", 1.0)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NearSphereOperation);
        NearSphereOperation ns = (NearSphereOperation) operation;
        assertEquals("location", ns.getFieldName());
        assertEquals(40.0, ns.getLongitude());
        assertEquals(70.0, ns.getLatitude());
        // Radians to meters: 6371000 * distance
        assertEquals(6371000 * 10.0, ns.getMaxDistance());
        assertEquals(6371000 * 1.0, ns.getMinDistance());
    }

    @Test
    void testParseNearSphereGeoJson() {
        Document geometry = new Document("type", "Point")
                .append("coordinates", Arrays.asList(40.0, 70.0));
        Document query = new Document(
                "location",
                new Document("$nearSphere", new Document("$geometry", geometry)
                        .append("$maxDistance", 1000.0)
                        .append("$minDistance", 100.0))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NearSphereOperation);
        NearSphereOperation ns = (NearSphereOperation) operation;
        assertEquals("location", ns.getFieldName());
        assertEquals(40.0, ns.getLongitude());
        assertEquals(70.0, ns.getLatitude());
        assertEquals(1000.0, ns.getMaxDistance());
        assertEquals(100.0, ns.getMinDistance());
    }

    @Test
    void testParseImplicitEquals() {
        Document query = new Document("age", 30);
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldName());
        assertEquals(30, eq.getValue());
    }

    @Test
    void testParseImplicitEqualsNull() {
        Document query = new Document("age", null);
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldName());
        assertEquals(null, eq.getValue());
    }

    @Test
    void testParseImplicitAnd() {
        Document query = new Document("age", 30).append("name", "John");
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        assertTrue(and.getConditions().get(0) instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) and.getConditions().get(0);
        assertEquals("age", eq.getFieldName());
        assertEquals(30, eq.getValue());

        assertTrue(and.getConditions().get(1) instanceof EqualsOperation);
        EqualsOperation<?> eq2 = (EqualsOperation<?>) and.getConditions().get(1);
        assertEquals("name", eq2.getFieldName());
        assertEquals("John", eq2.getValue());
    }

    @Test
    void testParseImplicitAndEquals() {
        Document query = new Document(
                "age",
                new Document("$gte", 18)
                        .append("$lt", 65)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        assertTrue(and.getConditions().get(0) instanceof GreaterThanEqualsOperation);
        GreaterThanEqualsOperation<?> gte = (GreaterThanEqualsOperation<?>) and.getConditions().get(0);
        assertEquals("age", gte.getFieldName());
        assertEquals(18, gte.getValue());

        assertTrue(and.getConditions().get(1) instanceof LessThanOperation);
        LessThanOperation<?> lt = (LessThanOperation<?>) and.getConditions().get(1);
        assertEquals("age", lt.getFieldName());
        assertEquals(65, lt.getValue());
    }


    @Test
    void testParseImplicitEqualsWithDocument() {
        Document innerDoc = new Document("foo", "bar");
        Document query = new Document("metadata", innerDoc);
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("metadata", eq.getFieldName());
        assertEquals(innerDoc, eq.getValue());
    }

    @Test
    void testParseMultipleOperatorsOnSameField() {
        Document query = new Document(
                "age",
                new Document("$gt", 18).append("$lt", 30)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        assertTrue(and.getConditions().get(0) instanceof GreaterThanOperation);
        assertTrue(and.getConditions().get(1) instanceof LessThanOperation);
    }

    @Test
    void testParseComplexNestedQuery() {
        Document query = new Document("$and",
                Arrays.asList(
                        new Document("age", new Document("$gte", 18)),
                        new Document("$or",
                                Arrays.asList(
                                        new Document("status", "active"),
                                        new Document("verified", true)))));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        assertTrue(and.getConditions().get(0) instanceof GreaterThanEqualsOperation);
        assertTrue(and.getConditions().get(1) instanceof OrOperation);

        OrOperation or = (OrOperation) and.getConditions().get(1);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    void testParseImplicitEqualsWithMultipleFields() {
        Document query = new Document("age", 30).append("city", "London");
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        // Assert that both are EqualsOperations
        assertTrue(and.getConditions().stream().allMatch(c -> c instanceof EqualsOperation));
    }

    @Test
    void testParseImplicitEqualsWithEmptyDocument() {
        Document query = new Document();
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseInvalidUnknownOperator() {
        Document query = new Document("age", new Document("$unknown", 30));
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseInvalidTopLevelNotQuery() {
        Document query = new Document("$not", 30);

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidTopLevelNotWithDocument() {
        Document query = new Document(
                "$not",
                new Document("age", new Document("$eq", 30))
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidNotWithLiteralValue() {
        Document query = new Document(
                "country",
                new Document("$not", "USA")
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidNotWithNumericLiteral() {
        Document query = new Document(
                "age",
                new Document("$not", 30)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidNotWithNullValue() {
        Document query = new Document(
                "age",
                new Document("$not", null)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidNotWithAndOperator() {
        Document query = new Document(
                "age",
                new Document(
                        "$not",
                        new Document(
                                "$and",
                                Arrays.asList(
                                        new Document("age", new Document("$lt", 18)),
                                        new Document("country", "USA")))
                )
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidNotWithOrOperator() {
        Document query = new Document(
                "age",
                new Document(
                        "$not",
                        new Document(
                                "$or",
                                new Document("age", new Document("$lt", 18))
                                        .append("country", "USA"))));
        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidUnknownTopLevelOperator() {
        Document query = new Document(
                "$unknown",
                new Document("age", 30)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidUnknownFieldOperator() {
        Document query = new Document(
                "age",
                new Document("$unknown", 30)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidAndWithNonArrayValue() {
        Document query = new Document(
                "$and",
                new Document("age", 30)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidOrWithNonArrayValue() {
        Document query = new Document("$or", 30);

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidNorWithNonArrayValue() {
        Document query = new Document("$nor", "invalid");

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidAndWithLiteralElement() {
        Document query = new Document(
                "$and",
                Arrays.asList(
                        new Document("age", 30),
                        "invalid"
                )
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidOrWithNumericElement() {
        Document query = new Document(
                "$or",
                Arrays.asList(
                        new Document("age", 30),
                        42
                )
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidEmptyOperatorDocument() {
        Document query = new Document(
                "age",
                new Document()
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidComparisonOperatorWithMissingValue() {
        Document query = new Document(
                "age",
                new Document("$lt", null)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidLessThanNull() {
        Document query = new Document(
                "age",
                new Document("$lt", null)
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseInvalidLessEqualsThanNull() {
        Document query = new Document(
                "age",
                new Document("$lte", null)
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseGreaterThenEqualsThanNull() {
        Document query = new Document(
                "age",
                new Document("$gte", null)
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseGreaterThenThanNull() {
        Document query = new Document(
                "age",
                new Document("$gt", null)
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }


    @Test
    void testParseInvalidMultipleOperatorsIncludingUnknownOperator() {
        Document query = new Document(
                "age",
                new Document("$gte", 18)
                        .append("$unknown", 30)
        );

        QueryOperation operation = parser.parse(query);

        assertNull(operation);
    }

    @Test
    void testParseInvalidExistsNull() {
        Document query = new Document(
                "age",
                new Document("$exists", null)
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseInvalidEmptyAnd() {
        Document query = new Document(
                "$and",
                new ArrayList<Document>()
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseInvalidEmptyOr() {
        Document query = new Document(
                "$or",
                new ArrayList<Document>()
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }
    @Test
    void testParseInvalidEmptyNor() {
        Document query = new Document(
                "$nor",
                new ArrayList<Document>()
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseEmptyAll() {
        Document query = new Document(
                "results",
                new Document("$all", new ArrayList<Document>())
        );
        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);

        assertTrue(operation instanceof AllOperation);
        AllOperation<?> all = (AllOperation<?>) operation;
        assertEquals("results", all.getFieldName());
        assertEquals(new ArrayList<>(), all.getValues());

    }

}
