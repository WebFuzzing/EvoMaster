package org.evomaster.client.java.controller.mongo;

import com.mongodb.client.model.Filters;
import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.BsonRegularExpression;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.selectors.TypeSelector;
import org.evomaster.client.java.controller.mongo.utils.BitmaskUtils;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest {

    private final QueryParser parser = new QueryParser();

    @Test
    void testParseElemMatchWithinAll() {
        // { "a": { "$all": [ { "$elemMatch": { "$gt": 2 } } ] } }
        Document query = new Document().append("a", new Document().append("$all",
                Collections.singletonList(new Document().append("$elemMatch",
                        new Document().append("$gt", 2)))));
        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof AllOperation);
        AllOperation<?> allOperation = (AllOperation<?>) operation;
        assertEquals("a", allOperation.getFieldPath());
        assertEquals(1, allOperation.getValues().size());
        Object elemMatchValue = allOperation.getValues().get(0);
        assertTrue(elemMatchValue instanceof ElemMatchOperation);
        ElemMatchOperation elemMatchOperation = (ElemMatchOperation) elemMatchValue;
        assertEquals("a", elemMatchOperation.getFieldPath());
        assertTrue(elemMatchOperation.getCondition() instanceof GreaterThanOperation);
        assertEquals(2, ((GreaterThanOperation) elemMatchOperation.getCondition()).getValue());
    }

    @Test
    void testParseEquals() {
        Document query = new Document(
                "age",
                new Document("$eq", 30)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldPath());
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
        assertEquals("age", eq.getFieldPath());
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
        assertEquals("age", ne.getFieldPath());
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
        assertEquals("age", ne.getFieldPath());
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
        assertEquals("age", in.getFieldPath());
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
        assertEquals("age", in.getFieldPath());
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
        assertEquals("age", exists.getFieldPath());
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
        assertEquals("age", not.getFieldPath());
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
        assertEquals("tags", size.getFieldPath());
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
        assertEquals("tags", all.getFieldPath());
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
        assertEquals("results", elemMatch.getFieldPath());
        // Implicit equals inside elemMatch
        assertTrue(elemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation equals = (EqualsOperation) elemMatch.getCondition();
        assertEquals("product", equals.getFieldPath());
        assertEquals("abc", equals.getValue());
    }

    @Test
    void testParseNestedElemMatch() {
        Document query = new Document(
                "groups",
                new Document("$elemMatch",
                        new Document("members",
                                new Document("$elemMatch", new Document("name", "Alice"))))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation outerElemMatch = (ElemMatchOperation) operation;
        assertEquals("groups", outerElemMatch.getFieldPath());

        assertTrue(outerElemMatch.getCondition() instanceof ElemMatchOperation);
        ElemMatchOperation innerElemMatch = (ElemMatchOperation) outerElemMatch.getCondition();
        assertEquals("members", innerElemMatch.getFieldPath());

        assertTrue(innerElemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation<?> equals = (EqualsOperation<?>) innerElemMatch.getCondition();
        assertEquals("name", equals.getFieldPath());
        assertEquals("Alice", equals.getValue());
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
        assertEquals("results", elemMatch.getFieldPath());
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
        assertEquals("results", elemMatch.getFieldPath());
        // Implicit equals inside elemMatch
        assertTrue(elemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation equals = (EqualsOperation) elemMatch.getCondition();
        assertEquals("product", equals.getFieldPath());
        assertEquals(null, equals.getValue());
    }

    @Test
    void testParseElemMatchWithEqOperator() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch", new Document("$eq", "b"))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("tags", elemMatch.getFieldPath());
        assertTrue(elemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation<?> equals = (EqualsOperation<?>) elemMatch.getCondition();
        assertEquals("$", equals.getFieldPath());
        assertEquals("b", equals.getValue());
    }

    @Test
    void testParseElemMatchWithGteOperator() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch", new Document("$gte", "b"))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("tags", elemMatch.getFieldPath());
        assertTrue(elemMatch.getCondition() instanceof GreaterThanEqualsOperation);
        GreaterThanEqualsOperation<?> greaterThanEquals =
                (GreaterThanEqualsOperation<?>) elemMatch.getCondition();
        assertEquals("$", greaterThanEquals.getFieldPath());
        assertEquals("b", greaterThanEquals.getValue());
    }

    @Test
    void testParseElemMatchWithAndBetweenLtAndGt() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch",
                        new Document("$and", Arrays.asList(
                                new Document("$lt", "c"),
                                new Document("$gt", "a"))))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("tags", elemMatch.getFieldPath());

        assertTrue(elemMatch.getCondition() instanceof AndOperation);
        AndOperation and = (AndOperation) elemMatch.getCondition();
        assertEquals(2, and.getConditions().size());

        assertTrue(and.getConditions().get(0) instanceof LessThanOperation);
        LessThanOperation<?> lessThan = (LessThanOperation<?>) and.getConditions().get(0);
        assertEquals("$", lessThan.getFieldPath());
        assertEquals("c", lessThan.getValue());

        assertTrue(and.getConditions().get(1) instanceof GreaterThanOperation);
        GreaterThanOperation<?> greaterThan = (GreaterThanOperation<?>) and.getConditions().get(1);
        assertEquals("$", greaterThan.getFieldPath());
        assertEquals("a", greaterThan.getValue());
    }

    @Test
    void testParseElemMatchWithOrCondition() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch",
                        new Document("$or", Arrays.asList(
                                new Document("$eq", "a"),
                                new Document("$eq", "b"))))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("tags", elemMatch.getFieldPath());

        assertTrue(elemMatch.getCondition() instanceof OrOperation);
        OrOperation or = (OrOperation) elemMatch.getCondition();
        assertEquals(2, or.getConditions().size());

        assertTrue(or.getConditions().get(0) instanceof EqualsOperation);
        EqualsOperation<?> firstEquals = (EqualsOperation<?>) or.getConditions().get(0);
        assertEquals("$", firstEquals.getFieldPath());
        assertEquals("a", firstEquals.getValue());

        assertTrue(or.getConditions().get(1) instanceof EqualsOperation);
        EqualsOperation<?> secondEquals = (EqualsOperation<?>) or.getConditions().get(1);
        assertEquals("$", secondEquals.getFieldPath());
        assertEquals("b", secondEquals.getValue());
    }

    @Test
    void testParseType() {
        Document query = new Document(
                "name",
                new Document("$type", "string")
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof TypeOperation);
        TypeOperation type = (TypeOperation) operation;
        assertEquals("name", type.getFieldPath());
        assertNotNull(type.getBsonTypes());
        List<Object> expectedBsonTypes = TypeSelector.parseToBsonTypes("string");
        assertEquals(expectedBsonTypes, type.getBsonTypes());
    }

    @Test
    void testParseInvalidType() {
        Document query = new Document(
                "name",
                new Document("$type", "STRING")
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
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
        assertEquals("name", type.getFieldPath());
        assertNotNull(type.getBsonTypes());
    }

    @Test
    void testParseRegexString() {
        Document query = new Document("name", new Document("$regex", "^hospital.*"));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertEquals("name", regex.getFieldPath());
        assertEquals("^hospital.*", regex.getPattern().pattern());
        assertTrue(regex.getOptions().isEmpty());
    }

    @Test
    void testParseRegexOptionsAreOptional() {
        Document query = new Document("name", new Document("$regex", "hospital"));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertNotNull(regex.getOptions());
        assertTrue(regex.getOptions().isEmpty());
        assertFalse(regex.getOptions().isCaseInsensitive());
        assertFalse(regex.getOptions().isMultiline());
        assertFalse(regex.getOptions().isDotAll());
        assertFalse(regex.getOptions().isExtended());
        assertFalse(regex.getOptions().isUnicode());
    }

    @Test
    void testParseRegexStringWithOptions() {
        Document query = new Document("name",
                new Document("$regex", "^hospital.*")
                        .append("$options", "imsxu"));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertEquals("name", regex.getFieldPath());
        assertEquals("^hospital.*", regex.getPattern().pattern());
        assertTrue(regex.getOptions().isCaseInsensitive());
        assertTrue(regex.getOptions().isMultiline());
        assertTrue(regex.getOptions().isDotAll());
        assertTrue(regex.getOptions().isExtended());
        assertTrue(regex.getOptions().isUnicode());
        assertTrue((regex.getPattern().flags() & Pattern.CASE_INSENSITIVE) != 0);
        assertTrue((regex.getPattern().flags() & Pattern.MULTILINE) != 0);
    }

    @Test
    void testParseRegexBsonRegularExpression() {
        Document query = new Document("name",
                new Document("$regex", new BsonRegularExpression("hospital$", "i")));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertEquals("hospital$", regex.getPattern().pattern());
        assertTrue(regex.getOptions().isCaseInsensitive());
        assertFalse(regex.getOptions().isMultiline());
    }

    @Test
    void testParseNotRegexBsonRegularExpression() {
        Document query = new Document("name",
                new Document("$not", new BsonRegularExpression("x")));

        NotOperation notOperation = assertInstanceOf(NotOperation.class, parser.parse(query));
        assertEquals("name", notOperation.getFieldPath());
        assertTrue(notOperation.getCondition() instanceof RegexOperation);
        RegexOperation regex = (RegexOperation) notOperation.getCondition();
        assertEquals("x", regex.getPattern().pattern());
    }

    @Test
    void testParseEqBsonRegularExpression() {
        Document query = new Document("name",
                new Document("$eq", new BsonRegularExpression("x")));

        assertNotNull(parser.parse(query));
    }

    @Test
    void testParseFiltersRegex() {
        Document query = convertToDocument(Filters.regex("name", "^hospital.*", "i"));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertEquals("name", regex.getFieldPath());
        assertEquals("^hospital.*", regex.getPattern().pattern());
        assertTrue(regex.getOptions().isCaseInsensitive());
        assertTrue((regex.getPattern().flags() & Pattern.CASE_INSENSITIVE) != 0);
    }

    @Test
    void testParseFiltersRegexWithJavaPattern() {
        Pattern pattern = Pattern.compile("^hospital.*near$", Pattern.MULTILINE | Pattern.DOTALL);
        Document query = convertToDocument(Filters.regex("description", pattern));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertEquals("description", regex.getFieldPath());
        assertEquals(pattern.pattern(), regex.getPattern().pattern());
        assertTrue(regex.getOptions().isMultiline());
        assertTrue(regex.getOptions().isDotAll());
        assertTrue((regex.getPattern().flags() & Pattern.MULTILINE) != 0);
        assertTrue((regex.getPattern().flags() & Pattern.DOTALL) != 0);
    }

    @Test
    void testParseRegexJavaPatternWithOptionsBeforeRegex() {
        Document query = new Document("name",
                new Document("$options", "s")
                        .append("$regex", Pattern.compile("hospital.*near", Pattern.CASE_INSENSITIVE)));

        RegexOperation regex = assertInstanceOf(RegexOperation.class, parser.parse(query));
        assertEquals("hospital.*near", regex.getPattern().pattern());
        assertTrue(regex.getOptions().isDotAll());
        assertFalse(regex.getOptions().isCaseInsensitive());
        assertTrue((regex.getPattern().flags() & Pattern.DOTALL) != 0);
        assertEquals(0, regex.getPattern().flags() & Pattern.CASE_INSENSITIVE);
    }

    @Test
    void testParseRegexRejectsInvalidQueries() {
        assertNull(parser.parse(new Document("name", new Document("$regex", 42))));
        assertNull(parser.parse(new Document("name",
                new Document("$regex", "hospital").append("$options", "g"))));
        assertNull(parser.parse(new Document("name",
                new Document("$regex", "[").append("$options", "i"))));
        assertNull(parser.parse(new Document("name",
                new Document("$regex", "hospital").append("$options", 1))));
    }

    @Test
    void testParseRegexRejectsNullPattern() {
        Document query = new Document("name", new Document("$regex", null));

        assertNull(parser.parse(query));
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
        assertEquals("age", gt.getFieldPath());
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
        assertEquals("age", lte.getFieldPath());
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
        assertEquals("age", nin.getFieldPath());
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
        assertEquals("age", mod.getFieldPath());
        assertEquals(2L, mod.getDivisor());
        assertEquals(0L, mod.getRemainder());
    }

    @Test
    void testModRejectZeroDivisor() {
        Document query = new Document(
                "age",
                new Document("$mod", Arrays.asList(0L, 0L))
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseBitsAllClearLong() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllClear", 5L)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllClearOperation);
        BitsAllClearOperation bitsAllClear = (BitsAllClearOperation) operation;
        assertEquals("flags", bitsAllClear.getFieldPath());
        assertEquals(5L, bitsAllClear.getBitmask());
    }

    @Test
    void testParseBitsAllClearInteger() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllClear", 5)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllClearOperation);
        BitsAllClearOperation bitsAllClear = (BitsAllClearOperation) operation;
        assertEquals("flags", bitsAllClear.getFieldPath());
        assertEquals(5L, bitsAllClear.getBitmask());
    }

    @Test
    void testParseBitsAllClearDouble() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllClear", 5.0d)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllClearOperation);
        BitsAllClearOperation bitsAllClear = (BitsAllClearOperation) operation;
        assertEquals("flags", bitsAllClear.getFieldPath());
        assertEquals(5L, bitsAllClear.getBitmask());
    }

    @Test
    void testParseBitsAllClearBigDecimal() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllClear", new Decimal128(5))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllClearOperation);
        BitsAllClearOperation bitsAllClear = (BitsAllClearOperation) operation;
        assertEquals("flags", bitsAllClear.getFieldPath());
        assertEquals(5L, bitsAllClear.getBitmask());
    }

    @Test
    void testParseBitsAllSetInteger() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllSet", 5)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllSetOperation);
        BitsAllSetOperation bitsAllSet = (BitsAllSetOperation) operation;
        assertEquals("flags", bitsAllSet.getFieldPath());
        assertEquals(5L, bitsAllSet.getBitmask());
    }

    @Test
    void testParseInvalidFractionalBitmask() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllSet", 3.5d)
        );
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseBitsAnyClearLong() {
        Document query = new Document(
                "flags",
                new Document("$bitsAnyClear", 5L)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAnyClearOperation);
        BitsAnyClearOperation bitsAnyClear = (BitsAnyClearOperation) operation;
        assertEquals("flags", bitsAnyClear.getFieldPath());
        assertEquals(5L, bitsAnyClear.getBitmask());
    }

    @Test
    void testParseBitsAnyClearInteger() {
        Document query = new Document(
                "flags",
                new Document("$bitsAnyClear", 5)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAnyClearOperation);
        BitsAnyClearOperation bitsAnyClear = (BitsAnyClearOperation) operation;
        assertEquals("flags", bitsAnyClear.getFieldPath());
        assertEquals(5L, bitsAnyClear.getBitmask());
    }

    @Test
    void testParseBitsAnySetLong() {
        Document query = new Document(
                "flags",
                new Document("$bitsAnySet", 5L)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAnySetOperation);
        BitsAnySetOperation bitsAnySet = (BitsAnySetOperation) operation;
        assertEquals("flags", bitsAnySet.getFieldPath());
        assertEquals(5L, bitsAnySet.getBitmask());
    }

    @Test
    void testParseBitsAnySetBigDecimal128() {
        Document query = new Document(
                "flags",
                new Document("$bitsAnySet", new Decimal128(5))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAnySetOperation);
        BitsAnySetOperation bitsAnySet = (BitsAnySetOperation) operation;
        assertEquals("flags", bitsAnySet.getFieldPath());
        assertEquals(5L, bitsAnySet.getBitmask());
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
        assertEquals("age", gte.getFieldPath());
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
        assertEquals("location", ns.getFieldPath());
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
        assertEquals("location", ns.getFieldPath());
        assertEquals(40.0, ns.getLongitude());
        assertEquals(70.0, ns.getLatitude());
        assertEquals(1000.0, ns.getMaxDistance());
        assertEquals(100.0, ns.getMinDistance());
    }

    @Test
    void testParseNearLegacy() {
        Document query = new Document(
                "location",
                new Document("$near", new Document("x", 40.0).append("y", 70.0))
                        .append("$maxDistance", 10.0)
                        .append("$minDistance", 1.0)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NearOperation);
        NearOperation near = (NearOperation) operation;
        assertEquals("location", near.getFieldPath());
        assertEquals(40.0, near.getLongitude());
        assertEquals(70.0, near.getLatitude());
        assertEquals(10.0, near.getMaxDistance());
        assertEquals(1.0, near.getMinDistance());
    }

    @Test
    void testParseNearGeoJson() {
        Document geometry = new Document("type", "Point")
                .append("coordinates", Arrays.asList(40.0, 70.0));
        Document query = new Document(
                "location",
                new Document("$near", new Document("$geometry", geometry)
                        .append("$maxDistance", 1000.0)
                        .append("$minDistance", 100.0))
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NearOperation);
        NearOperation near = (NearOperation) operation;
        assertEquals("location", near.getFieldPath());
        assertEquals(40.0, near.getLongitude());
        assertEquals(70.0, near.getLatitude());
        assertEquals(1000.0, near.getMaxDistance());
        assertEquals(100.0, near.getMinDistance());
    }

    @Test
    void testParseNearLegacyCoordinateArray() {
        Document query = new Document("location",
                new Document("$near", Arrays.asList(40.0, 70.0))
                        .append("$maxDistance", 10.0));

        NearOperation near = assertInstanceOf(NearOperation.class, parser.parse(query));
        assertEquals(40.0, near.getLongitude());
        assertEquals(70.0, near.getLatitude());
        assertEquals(10.0, near.getMaxDistance());
        assertNull(near.getMinDistance());
    }

    @Test
    void testParseNearSphereLegacyCoordinateArray() {
        Document query = new Document("location",
                new Document("$nearSphere", Arrays.asList(40.0, 70.0))
                        .append("$minDistance", 0.25));

        NearSphereOperation near = assertInstanceOf(NearSphereOperation.class, parser.parse(query));
        assertEquals(40.0, near.getLongitude());
        assertEquals(70.0, near.getLatitude());
        assertNull(near.getMaxDistance());
        assertEquals(6371000 * 0.25, near.getMinDistance());
    }

    @Test
    void testParseNearWithoutDistances() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            Document legacyQuery = new Document("location",
                    new Document(operator, Arrays.asList(40.0, 70.0)));
            AbstractProximityOperation legacy = assertProximityOperation(operator, parser.parse(legacyQuery));
            assertNull(legacy.getMinDistance());
            assertNull(legacy.getMaxDistance());

            Document geometry = new Document("type", "Point")
                    .append("coordinates", Arrays.asList(40.0, 70.0));
            Document geoJsonQuery = new Document("location",
                    new Document(operator, new Document("$geometry", geometry)));
            AbstractProximityOperation geoJson = assertProximityOperation(operator, parser.parse(geoJsonQuery));
            assertNull(geoJson.getMinDistance());
            assertNull(geoJson.getMaxDistance());
        }
    }

    @Test
    void testParseNearWithIndividualDistanceLimits() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            Document withMaxDistance = new Document("location",
                    new Document(operator, Arrays.asList(40.0, 70.0))
                            .append("$maxDistance", 0.5));
            AbstractProximityOperation legacy = assertProximityOperation(operator, parser.parse(withMaxDistance));
            assertNotNull(legacy.getMaxDistance());
            assertNull(legacy.getMinDistance());

            Document geometry = new Document("type", "Point")
                    .append("coordinates", Arrays.asList(40.0, 70.0));
            Document withMinDistance = new Document("location",
                    new Document(operator, new Document("$geometry", geometry)
                            .append("$minDistance", 100)));
            AbstractProximityOperation geoJson = assertProximityOperation(operator, parser.parse(withMinDistance));
            assertEquals(100.0, geoJson.getMinDistance());
            assertNull(geoJson.getMaxDistance());
        }
    }

    @Test
    void testParseNearAcceptsNumericBsonVariants() {
        Document geometry = new Document("type", "Point")
                .append("coordinates", Arrays.asList(40, 70L));
        Document query = new Document("location",
                new Document("$near", new Document("$geometry", geometry)
                        .append("$minDistance", 100)
                        .append("$maxDistance", 1000L)));

        NearOperation near = assertInstanceOf(NearOperation.class, parser.parse(query));
        assertEquals(40.0, near.getLongitude());
        assertEquals(70.0, near.getLatitude());
        assertEquals(100.0, near.getMinDistance());
        assertEquals(1000.0, near.getMaxDistance());
    }

    @Test
    void testParseNearOperatorDoesNotNeedToBeFirst() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            Document query = new Document("location",
                    new Document("$maxDistance", 0.5)
                            .append(operator, Arrays.asList(40.0, 70.0)));
            assertProximityOperation(operator, parser.parse(query));
        }
    }

    @Test
    void testParseNearGeoJsonBoundaryCoordinates() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            Document geometry = new Document("type", "Point")
                    .append("coordinates", Arrays.asList(-180, 90));
            Document query = new Document("location",
                    new Document(operator, new Document("$geometry", geometry)));

            AbstractProximityOperation near = assertProximityOperation(operator, parser.parse(query));
            assertEquals(-180.0, near.getLongitude());
            assertEquals(90.0, near.getLatitude());
        }
    }

    @Test
    void testParseNearRejectsInvalidLegacyCoordinates() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            assertNull(parser.parse(new Document("location",
                    new Document(operator, Collections.singletonList(40.0)))));
            assertNull(parser.parse(new Document("location",
                    new Document(operator, Arrays.asList(40.0, 70.0, 80.0)))));
            assertNull(parser.parse(new Document("location",
                    new Document(operator, Arrays.asList("40", 70.0)))));
        }
    }

    @Test
    void testParseNearRejectsInvalidGeoJsonPoints() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            assertInvalidGeoJsonNear(operator, new Document("type", "LineString")
                    .append("coordinates", Arrays.asList(40.0, 70.0)));
            assertInvalidGeoJsonNear(operator, new Document("type", "Point")
                    .append("coordinates", Collections.singletonList(40.0)));
            assertInvalidGeoJsonNear(operator, new Document("type", "Point")
                    .append("coordinates", Arrays.asList(181.0, 70.0)));
            assertInvalidGeoJsonNear(operator, new Document("type", "Point")
                    .append("coordinates", Arrays.asList(40.0, "70")));
            assertNull(parser.parse(new Document("location", new Document(operator, new Document()))));
        }
    }

    @Test
    void testParseNearRejectsNonNumericDistances() {
        for (String operator : Arrays.asList("$near", "$nearSphere")) {
            Document legacyQuery = new Document("location",
                    new Document(operator, Arrays.asList(40.0, 70.0))
                            .append("$maxDistance", "far"));
            assertNull(parser.parse(legacyQuery));

            Document geometry = new Document("type", "Point")
                    .append("coordinates", Arrays.asList(40.0, 70.0));
            Document geoJsonQuery = new Document("location",
                    new Document(operator, new Document("$geometry", geometry)
                            .append("$minDistance", "near")));
            assertNull(parser.parse(geoJsonQuery));
        }
    }

    private AbstractProximityOperation assertProximityOperation(String operator, QueryOperation operation) {
        return "$near".equals(operator)
                ? assertInstanceOf(NearOperation.class, operation)
                : assertInstanceOf(NearSphereOperation.class, operation);
    }

    private void assertInvalidGeoJsonNear(String operator, Document geometry) {
        Document query = new Document("location",
                new Document(operator, new Document("$geometry", geometry)));
        assertNull(parser.parse(query));
    }

    @Test
    void testParseImplicitEquals() {
        Document query = new Document("age", 30);
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldPath());
        assertEquals(30, eq.getValue());
    }

    @Test
    void testParseImplicitEqualsNull() {
        Document query = new Document("age", null);
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldPath());
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
        assertEquals("age", eq.getFieldPath());
        assertEquals(30, eq.getValue());

        assertTrue(and.getConditions().get(1) instanceof EqualsOperation);
        EqualsOperation<?> eq2 = (EqualsOperation<?>) and.getConditions().get(1);
        assertEquals("name", eq2.getFieldPath());
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
        assertEquals("age", gte.getFieldPath());
        assertEquals(18, gte.getValue());

        assertTrue(and.getConditions().get(1) instanceof LessThanOperation);
        LessThanOperation<?> lt = (LessThanOperation<?>) and.getConditions().get(1);
        assertEquals("age", lt.getFieldPath());
        assertEquals(65, lt.getValue());
    }


    @Test
    void testParseImplicitEqualsWithDocument() {
        Document innerDoc = new Document("foo", "bar");
        Document query = new Document("metadata", innerDoc);
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("metadata", eq.getFieldPath());
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
        assertTrue(operation instanceof EmptyOperation);
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
        assertNotNull(operation);
        assertTrue(operation instanceof ExistsOperation);
        ExistsOperation exists = (ExistsOperation) operation;
        assertEquals("age", exists.getFieldPath());
        assertFalse(exists.getBoolean());
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
        assertEquals("results", all.getFieldPath());
        assertEquals(new ArrayList<>(), all.getValues());

    }

    @Test
    void testParseTrueOperation() {
        Document query = new Document();

        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof EmptyOperation);

    }

    @Test
    void testParseNorTrueOperation() {
        Document query = new Document(
                "$nor",
                Collections.singletonList(new Document())
        );

        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof NorOperation);
        NorOperation nor = (NorOperation) operation;
        assertEquals(1, nor.getConditions().size());
        assertTrue(nor.getConditions().get(0) instanceof EmptyOperation);
    }

    @Test
    void testParseEqOperationWithList() {
        Document query = new Document(
                "f",
                new Document("$eq", Arrays.asList("Bob", "Alice")));
        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("f", eq.getFieldPath());
        assertEquals(Arrays.asList("Bob", "Alice"), eq.getValue());
    }

    @Test
    void testParseImplicitEqOperationWithList() {
        Document query = new Document(
                "f",
                Arrays.asList("Bob", "Alice"));
        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("f", eq.getFieldPath());
        assertEquals(Arrays.asList("Bob", "Alice"), eq.getValue());
    }

    @Test
    void testParseLessThan() {
        QueryOperation operation = parser.parse(
                new Document("age", new Document("$lt", 65))
        );

        assertTrue(operation instanceof LessThanOperation);
        LessThanOperation<?> lessThan = (LessThanOperation<?>) operation;
        assertEquals("age", lessThan.getFieldPath());
        assertEquals(65, lessThan.getValue());
    }

    @Test
    void testParseAttachedComments() {
        Document query = new Document("age", 42).append("$comments", "a comment");
        QueryOperation operation = parser.parse(query);

        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("age", eq.getFieldPath());
        assertEquals(42, eq.getValue());
    }

    @Test
    void testParseTopLevelDollar() {
        Document query = new Document("$foo", "bar");
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseOnlyComments() {
        Document query = new Document("$comments", "a comment");
        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testParseQueryWithComment() {
        Document query = new Document().append("a", 1).append("$comment", "note");

        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("a", eq.getFieldPath());
        assertEquals(1, eq.getValue());
    }

    @Test
    void testParseOnlyComment() {
        Document query = new Document("$comment", "a comment");
        QueryOperation operation = parser.parse(query);

        assertTrue(operation instanceof EmptyOperation);
    }

    @Test
    void testCommentAsField() {
        Document query =  new Document("foo" ,new Document("$comment", "bar"));
        QueryOperation operation = parser.parse(query);

        assertNotNull(operation);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("foo", eq.getFieldPath());
        assertEquals(new Document("$comment", "bar"), eq.getValue());
    }

    @Test
    void testParseAndWithOnlyCommentElement() {
        Document query = new Document(
                "$and",
                Collections.singletonList(new Document("$comment", "a comment"))
        );

        QueryOperation operation = parser.parse(query);

        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(1, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof EmptyOperation);
    }

    @Test
    void testParseNorWithOnlyCommentElement() {
        Document query = new Document(
                "$nor",
                Collections.singletonList(new Document("$comment", "a comment"))
        );

        QueryOperation operation = parser.parse(query);

        assertTrue(operation instanceof NorOperation);
        NorOperation norOperation = (NorOperation) operation;
        assertEquals(1, norOperation.getConditions().size());
        assertTrue(norOperation.getConditions().get(0) instanceof EmptyOperation);
    }

    @Test
    void testParseAttachedCommentInsideFieldOperatorDocument() {
        Document query = new Document(
                "age",
                new Document("$gt", 18).append("$comments", "a comment")
        );

        QueryOperation operation = parser.parse(query);

        assertTrue(operation instanceof GreaterThanOperation);
        GreaterThanOperation<?> gt = (GreaterThanOperation<?>) operation;
        assertEquals("age", gt.getFieldPath());
        assertEquals(18, gt.getValue());
    }

    @Test
    void testParseCommentsIgnoredRecursivelyInLogicalQuery() {
        Document query = new Document(
                "$and",
                Arrays.asList(
                        new Document("age", new Document("$gt", 18).append("$comments", "inner comment")),
                        new Document(
                                "$or",
                                Arrays.asList(
                                        new Document("name", "John").append("$comments", "leaf comment"),
                                        new Document("name", "Jane")
                                )
                        ).append("$comments", "or comment"),
                        new Document("$comments", "no-op comment")
                )
        ).append("$comments", "top-level comment");

        QueryOperation operation = parser.parse(query);

        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(3, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof GreaterThanOperation);
        assertTrue(and.getConditions().get(1) instanceof OrOperation);
        assertTrue(and.getConditions().get(2) instanceof EmptyOperation);

        OrOperation or = (OrOperation) and.getConditions().get(1);
        assertEquals(2, or.getConditions().size());
        assertTrue(or.getConditions().get(0) instanceof EqualsOperation);
        assertTrue(or.getConditions().get(1) instanceof EqualsOperation);
    }

    @Test
    void testParseExistsFalse() {
        QueryOperation operation = parser.parse(
                new Document("age", new Document("$exists", false))
        );

        assertTrue(operation instanceof ExistsOperation);
        assertFalse(((ExistsOperation) operation).getBoolean());
    }

    @Test
    void testParseNotWithComparisonOperator() {
        QueryOperation operation = parser.parse(
                new Document("age", new Document("$not", new Document("$gt", 18)))
        );

        assertTrue(operation instanceof NotOperation);
        NotOperation not = (NotOperation) operation;
        assertEquals("age", not.getFieldPath());
        assertTrue(not.getCondition() instanceof GreaterThanOperation);
        assertEquals(18, ((GreaterThanOperation<?>) not.getCondition()).getValue());
    }

    @Test
    void testParseValidNestedNotQuery() {
        Document query = new Document(
                "a",
                new Document("$not", new Document("$not", new Document("$gt", 1)))
        );

        QueryOperation operation = parser.parse(query);

        assertNotNull(operation);
        assertTrue(operation instanceof NotOperation);
        NotOperation outerNot = (NotOperation) operation;
        assertEquals("a", outerNot.getFieldPath());
        assertTrue(outerNot.getCondition() instanceof NotOperation);

        NotOperation innerNot = (NotOperation) outerNot.getCondition();
        assertEquals("a", innerNot.getFieldPath());
        assertTrue(innerNot.getCondition() instanceof GreaterThanOperation);
        assertEquals(1, ((GreaterThanOperation<?>) innerNot.getCondition()).getValue());
    }

    @Test
    void testParseInvalidNotWithMultipleComparisonOperators() {
        Document query = new Document(
                "a",
                new Document("$not", new Document("$gt", 1).append("$lt", 9))
        );

        QueryOperation operation = parser.parse(query);
        assertNotNull(operation);
        assertTrue(operation instanceof NotOperation);
        NotOperation not = (NotOperation) operation;
        assertEquals("a", not.getFieldPath());
        assertTrue(not.getCondition() instanceof AndOperation);
        AndOperation and = (AndOperation) not.getCondition();
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof GreaterThanOperation);
        assertTrue(and.getConditions().get(1) instanceof LessThanOperation);
    }

    @Test
    void testParseTopLevelValueOperatorWithSyntheticField() {
        QueryOperation operation = parser.parse(new Document("$gt", 18));

        assertTrue(operation instanceof GreaterThanOperation);
        GreaterThanOperation<?> greaterThan = (GreaterThanOperation<?>) operation;
        assertEquals("$", greaterThan.getFieldPath());
        assertEquals(18, greaterThan.getValue());
    }

    @Test
    void testParseElemMatchWithImplicitRange() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch",
                        new Document("$gt", "a").append("$lt", "c"))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        QueryOperation condition = ((ElemMatchOperation) operation).getCondition();
        assertTrue(condition instanceof AndOperation);

        AndOperation and = (AndOperation) condition;
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof GreaterThanOperation);
        assertTrue(and.getConditions().get(1) instanceof LessThanOperation);
        assertEquals("$", ((GreaterThanOperation<?>) and.getConditions().get(0)).getFieldPath());
        assertEquals("$", ((LessThanOperation<?>) and.getConditions().get(1)).getFieldPath());
    }

    @Test
    void testParseElemMatchWithNotEqualsOperator() {
        ElemMatchOperation elemMatch = parseElemMatchCondition(
                new Document("$ne", "b"),
                NotEqualsOperation.class
        );
        NotEqualsOperation<?> notEquals = (NotEqualsOperation<?>) elemMatch.getCondition();
        assertEquals("$", notEquals.getFieldPath());
        assertEquals("b", notEquals.getValue());
    }

    @Test
    void testParseElemMatchWithLessThanEqualsOperator() {
        ElemMatchOperation elemMatch = parseElemMatchCondition(
                new Document("$lte", "b"),
                LessThanEqualsOperation.class
        );
        LessThanEqualsOperation<?> lessThanEquals =
                (LessThanEqualsOperation<?>) elemMatch.getCondition();
        assertEquals("$", lessThanEquals.getFieldPath());
        assertEquals("b", lessThanEquals.getValue());
    }

    @Test
    void testParseTypeWithListOfTypes() {
        // mongo: matches, the field holds one of the listed types
        Document query = new Document().append("a",
                new Document().append("$type", Arrays.asList("string", "double")));

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof TypeOperation);
        TypeOperation type = (TypeOperation) operation;

        final List<Object> expectedBsonTypes = new LinkedList<>();
        expectedBsonTypes.addAll(TypeSelector.parseToBsonTypes("string"));
        expectedBsonTypes.addAll(TypeSelector.parseToBsonTypes("double"));
        assertEquals(expectedBsonTypes, type.getBsonTypes());
    }

    @Test
    void testParseAllTypes() {
        // mongo: matches, the field holds one of the listed types
        final List<String> aliases = Arrays.asList(
                "double",
                "string",
                "object",
                "array",
                "binData",
                "undefined",
                "objectId",
                "bool",
                "date",
                "null",
                "regex",
                "dbPointer",
                "javascript",
                "symbol",
                "javascriptWithScope",
                "int",
                "timestamp",
                "long",
                "decimal",
                "minKey",
                "maxKey",
                "number"
        );
        Document query = new Document().append("a",
                new Document().append("$type",
                        aliases));

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof TypeOperation);
        TypeOperation type = (TypeOperation) operation;

        final List<Object> expectedBsonTypes = new LinkedList<>();
        for (String alias : aliases) {
            expectedBsonTypes.addAll(TypeSelector.parseToBsonTypes(alias));
        }
        assertEquals(expectedBsonTypes, type.getBsonTypes());
    }

    @Test
    void testParseElemMatchWithInOperator() {
        ElemMatchOperation elemMatch = parseElemMatchCondition(
                new Document("$in", Arrays.asList("a", "b")),
                InOperation.class
        );
        InOperation<?> in = (InOperation<?>) elemMatch.getCondition();
        assertEquals("$", in.getFieldPath());
        assertEquals(Arrays.asList("a", "b"), in.getValues());
    }

    @Test
    void testParseElemMatchWithNotInOperator() {
        ElemMatchOperation elemMatch = parseElemMatchCondition(
                new Document("$nin", Arrays.asList("a", "b")),
                NotInOperation.class
        );
        NotInOperation<?> notIn = (NotInOperation<?>) elemMatch.getCondition();
        assertEquals("$", notIn.getFieldPath());
        assertEquals(Arrays.asList("a", "b"), notIn.getValues());
    }

    @Test
    void testParseElemMatchWithNotOperator() {
        ElemMatchOperation elemMatch = parseElemMatchCondition(
                new Document("$not", new Document("$eq", "b")),
                NotOperation.class
        );
        NotOperation not = (NotOperation) elemMatch.getCondition();
        assertEquals("$", not.getFieldPath());
        assertTrue(not.getCondition() instanceof EqualsOperation);
        assertEquals("b", ((EqualsOperation<?>) not.getCondition()).getValue());
    }

    @Test
    void testParseElemMatchWithNestedLogicalConditions() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch",
                        new Document("$and", Arrays.asList(
                                new Document("$gt", "a"),
                                new Document("$or", Arrays.asList(
                                        new Document("$eq", "b"),
                                        new Document("$eq", "c"))))))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        QueryOperation condition = ((ElemMatchOperation) operation).getCondition();
        assertTrue(condition instanceof AndOperation);
        AndOperation and = (AndOperation) condition;
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof GreaterThanOperation);
        assertTrue(and.getConditions().get(1) instanceof OrOperation);
        assertEquals(2, ((OrOperation) and.getConditions().get(1)).getConditions().size());
    }

    @Test
    void testParseElemMatchWithNorCondition() {
        Document query = new Document(
                "tags",
                new Document("$elemMatch",
                        new Document("$nor", Arrays.asList(
                                new Document("$eq", "a"),
                                new Document("$eq", "b"))))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        QueryOperation condition = ((ElemMatchOperation) operation).getCondition();
        assertTrue(condition instanceof NorOperation);
        assertEquals(2, ((NorOperation) condition).getConditions().size());
    }

    @Test
    void testParseElemMatchNestedInsideOr() {
        Document query = new Document(
                "$or",
                Arrays.asList(
                        new Document("tags", new Document("$elemMatch", new Document("$eq", "a"))),
                        new Document("tags", new Document("$elemMatch", new Document("$eq", "b"))))
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof OrOperation);
        OrOperation or = (OrOperation) operation;
        assertEquals(2, or.getConditions().size());
        assertTrue(or.getConditions().stream().allMatch(c -> c instanceof ElemMatchOperation));
    }

    @Test
    void testParseElemMatchWithEmptyCondition() {
        QueryOperation operation = parser.parse(
                new Document("tags", new Document("$elemMatch", new Document()))
        );

        assertTrue(operation instanceof ElemMatchOperation);
        assertTrue(((ElemMatchOperation) operation).getCondition() instanceof EmptyOperation);
    }

    @Test
    void testParseInvalidElemMatchConditions() {
        assertAll(
                () -> assertInvalidQuery(
                        new Document("tags", new Document("$elemMatch", "invalid"))),
                () -> assertInvalidQuery(
                        new Document("tags", new Document("$elemMatch", null))),
                () -> assertInvalidQuery(new Document(
                        "tags",
                        new Document("$elemMatch", new Document("$unknown", 1))
                )),
                () -> assertInvalidQuery(new Document(
                        "tags",
                        new Document("$elemMatch", new Document("$or", "invalid"))
                ))
        );
    }

    @Test
    void testParseInvalidCollectionOperatorValues() {
        assertAll(
                () -> assertInvalidQuery(new Document("tags", new Document("$in", "a"))),
                () -> assertInvalidQuery(new Document("tags", new Document("$nin", "a"))),
                () -> assertInvalidQuery(new Document("tags", new Document("$all", "a")))
        );
    }

    @Test
    void testParseInvalidModValues() {
        assertAll(
                () -> assertInvalidQuery(
                        new Document("age", new Document("$mod", Collections.emptyList()))),
                () -> assertInvalidQuery(
                        new Document("age", new Document("$mod", Collections.singletonList(2L)))),
                () -> assertInvalidQuery(
                        new Document("age", new Document("$mod", Arrays.asList(2L, 0L, 1L))))
        );
    }

    @Test
    void testParseModWithIntegerValues() {
        QueryOperation operation = parser.parse(
                new Document("age", new Document("$mod", Arrays.asList(2, 0)))
        );

        assertTrue(operation instanceof ModOperation);
        ModOperation mod = (ModOperation) operation;
        assertEquals(2L, mod.getDivisor());
        assertEquals(0L, mod.getRemainder());
    }

    @Test
    void testParseInvalidSizeValues() {
        assertAll(
                () -> assertInvalidQuery(new Document("tags", new Document("$size", "three"))),
                () -> assertInvalidQuery(new Document("tags", new Document("$size", -1)))
        );
    }

    @Test
    void testParseInvalidTypeValues() {
        assertAll(
                () -> assertInvalidQuery(
                        new Document("value", new Document("$type", "NOT_A_BSON_TYPE"))),
                () -> assertInvalidQuery(new Document("value", new Document("$type", 999)))
        );
    }

    @Test
    void testParseInvalidBitwiseValues() {
        assertAll(
                () -> assertInvalidQuery(
                        new Document("flags", new Document("$bitsAllClear", "5"))),
                () -> assertInvalidQuery(
                        new Document("flags", new Document("$bitsAnyClear", true))),
                () -> assertInvalidQuery(
                        new Document("flags", new Document("$bitsAnySet", new Document("bit", 1))))
        );
    }

    @Test
    void testParseNearSphereWithoutOptionalDistances() {
        Document geometry = new Document("type", "Point")
                .append("coordinates", Arrays.asList(40.0, 70.0));
        QueryOperation operation = parser.parse(
                new Document("location",
                        new Document("$nearSphere", new Document("$geometry", geometry)))
        );

        assertTrue(operation instanceof NearSphereOperation);
        NearSphereOperation nearSphere = (NearSphereOperation) operation;
        assertEquals(40.0, nearSphere.getLongitude());
        assertEquals(70.0, nearSphere.getLatitude());
        assertNull(nearSphere.getMaxDistance());
        assertNull(nearSphere.getMinDistance());
    }

    @Test
    void testParseInvalidNearSphereCoordinates() {
        Document geometryWithWrongCount = new Document("type", "Point")
                .append("coordinates", Collections.singletonList(40.0));
        Document geometryWithWrongTypes = new Document("type", "Point")
                .append("coordinates", Arrays.asList("40", 70.0));

        assertAll(
                () -> assertInvalidQuery(new Document(
                        "location",
                        new Document("$nearSphere", new Document("x", 40.0))
                )),
                () -> assertInvalidQuery(new Document(
                        "location",
                        new Document("$nearSphere", new Document("$geometry", geometryWithWrongCount))
                )),
                () -> assertInvalidQuery(new Document(
                        "location",
                        new Document("$nearSphere", new Document("$geometry", geometryWithWrongTypes))
                ))
        );
    }

    @Test
    void testParseNearSphereIndependentOfOperatorOrder() {
        Document point = new Document("x", 40.0).append("y", 70.0);
        Document nearSphere = new Document("$maxDistance", 10.0)
                .append("$nearSphere", point)
                .append("$minDistance", 1.0);

        QueryOperation operation = parser.parse(new Document("location", nearSphere));

        assertTrue(operation instanceof NearSphereOperation);
        NearSphereOperation parsed = (NearSphereOperation) operation;
        assertEquals(6371000 * 10.0, parsed.getMaxDistance());
        assertEquals(6371000 * 1.0, parsed.getMinDistance());
    }

    @Test
    void testParseMultipleOperatorsOnSameFieldInReverseOrder() {
        Document query = new Document(
                "age",
                new Document("$lt", 30).append("$gt", 18)
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());
        assertTrue(and.getConditions().get(0) instanceof LessThanOperation);
        assertTrue(and.getConditions().get(1) instanceof GreaterThanOperation);
    }

    @Test
    void testParseInvalidMixedOperatorAndFieldDocument() {
        assertInvalidQuery(new Document(
                "age",
                new Document("$gt", 18).append("unit", "years")
        ));
    }

    @Test
    void testParseInvalidNorWithInvalidCondition() {
        assertInvalidQuery(new Document(
                "$nor",
                Arrays.asList(new Document("age", 30), "invalid")
        ));
    }

    @Test
    void testBinaryBitmask() {
        Binary mask = new Binary(new byte[]{0x30}); // 0011 0000: bits 4 and 5

        Document query = new Document(
                "flags",
                new Document("$bitsAllSet", mask)
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllSetOperation);
        BitsAllSetOperation bitsAllSet = (BitsAllSetOperation) operation;
        OptionalLong expectedBitmask = BitmaskUtils.toBitMaskValue(mask);
        assertTrue(expectedBitmask.isPresent());
        assertEquals(expectedBitmask.getAsLong(), bitsAllSet.getBitmask());
    }

    @Test
    void testNegativeBitmask() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllSet", -1)
        );

        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testNegativeArrayBitmask() {
        Document query = new Document(
                "flags",
                new Document("$bitsAllSet", Arrays.asList(-1))
        );

        QueryOperation operation = parser.parse(query);
        assertNull(operation);
    }

    @Test
    void testBsonBinaryBitmask() {
        BsonBinary mask = new BsonBinary(new byte[]{0x30}); // 0011 0000: bits 4 and 5

        Document query = new Document(
                "flags",
                new Document("$bitsAllSet", mask)
        );

        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof BitsAllSetOperation);
        BitsAllSetOperation bitsAllSet = (BitsAllSetOperation) operation;
        OptionalLong expectedBitmask = BitmaskUtils.toBitMaskValue(mask);
        assertTrue(expectedBitmask.isPresent());
        assertEquals(expectedBitmask.getAsLong(), bitsAllSet.getBitmask());
    }

    @Test
    void testParseGreaterThanOrEqualsNull() {
        Document query = new Document(
                "age",
                new Document("$gte", null)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof GreaterThanEqualsOperation);
        GreaterThanEqualsOperation<?> gte = (GreaterThanEqualsOperation<?>) operation;
        assertEquals("age", gte.getFieldPath());
        assertEquals(null, gte.getValue());
    }

    @Test
    void testParseGreaterThanNull() {
        Document query = new Document(
                "age",
                new Document("$gt", null)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof GreaterThanOperation);
        GreaterThanOperation<?> gt = (GreaterThanOperation<?>) operation;
        assertEquals("age", gt.getFieldPath());
        assertEquals(null, gt.getValue());
    }

    @Test
    void testParseLesserThanNull() {
        Document query = new Document(
                "age",
                new Document("$lt", null)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof LessThanOperation);
        LessThanOperation<?> lt = (LessThanOperation<?>) operation;
        assertEquals("age", lt.getFieldPath());
        assertEquals(null, lt.getValue());
    }

    @Test
    void testParseLesserThanOrEqualNull() {
        Document query = new Document(
                "age",
                new Document("$lte", null)
        );
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof LessThanEqualsOperation);
        LessThanEqualsOperation<?> lte = (LessThanEqualsOperation<?>) operation;
        assertEquals("age", lte.getFieldPath());
        assertEquals(null, lte.getValue());
    }


    @Test
    void testParseImplicitEqualsWithDotNotation() {
        // { "address.city": "Paris" }
        Document query = new Document("address.city", "Paris");
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("address.city", eq.getFieldPath());
        assertEquals("Paris", eq.getValue());
    }

    @Test
    void testParseExplicitEqualsWithDotNotation() {
        // { "address.city": { "$eq": "Paris" } }
        Document query = new Document("address.city", new Document("$eq", "Paris"));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("address.city", eq.getFieldPath());
        assertEquals("Paris", eq.getValue());
    }

    @Test
    void testParseDeeplyNestedDotNotation() {
        // { "a.b.c.d": { "$ne": 5 } }
        Document query = new Document("a.b.c.d", new Document("$ne", 5));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NotEqualsOperation);
        NotEqualsOperation<?> ne = (NotEqualsOperation<?>) operation;
        assertEquals("a.b.c.d", ne.getFieldPath());
        assertEquals(5, ne.getValue());
    }

    @Test
    void testParseDotNotationWithArrayIndex() {
        // { "items.0.price": { "$gt": 10 } }
        Document query = new Document("items.0.price", new Document("$gt", 10));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof GreaterThanOperation);
        GreaterThanOperation<?> gt = (GreaterThanOperation<?>) operation;
        assertEquals("items.0.price", gt.getFieldPath());
        assertEquals(10, gt.getValue());
    }

    @Test
    void testParseDotNotationWithTrailingArrayIndex() {
        // { "tags.1": "blue" }
        Document query = new Document("tags.1", "blue");
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("tags.1", eq.getFieldPath());
        assertEquals("blue", eq.getValue());
    }

    @Test
    void testParseDotNotationVersusEmbeddedDocument() {
        // { "address": { "city": "Paris" } } is an exact match on an embedded document,
        // whereas { "address.city": "Paris" } matches a single nested field.
        Document embedded = new Document("address", new Document("city", "Paris"));
        QueryOperation embeddedOperation = parser.parse(embedded);
        assertTrue(embeddedOperation instanceof EqualsOperation);
        EqualsOperation<?> embeddedEq = (EqualsOperation<?>) embeddedOperation;
        assertEquals("address", embeddedEq.getFieldPath());
        assertEquals(new Document("city", "Paris"), embeddedEq.getValue());

        Document dotted = new Document("address.city", "Paris");
        QueryOperation dottedOperation = parser.parse(dotted);
        assertTrue(dottedOperation instanceof EqualsOperation);
        EqualsOperation<?> dottedEq = (EqualsOperation<?>) dottedOperation;
        assertEquals("address.city", dottedEq.getFieldPath());
        assertEquals("Paris", dottedEq.getValue());
    }

    @Test
    void testParseEmbeddedDocumentValueWithDottedKeyIsLiteral() {
        // { "address": { "geo.lat": 1 } }: dotted key inside a value is kept as a literal document
        Document query = new Document("address", new Document("geo.lat", 1));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) operation;
        assertEquals("address", eq.getFieldPath());
        assertEquals(new Document("geo.lat", 1), eq.getValue());
    }

    @Test
    void testParseRangeWithDotNotation() {
        // { "stats.score": { "$gte": 18, "$lt": 65 } }
        Document query = new Document("stats.score", new Document("$gte", 18).append("$lt", 65));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        GreaterThanEqualsOperation<?> gte = (GreaterThanEqualsOperation<?>) and.getConditions().get(0);
        assertEquals("stats.score", gte.getFieldPath());
        assertEquals(18, gte.getValue());

        LessThanOperation<?> lt = (LessThanOperation<?>) and.getConditions().get(1);
        assertEquals("stats.score", lt.getFieldPath());
        assertEquals(65, lt.getValue());
    }

    @Test
    void testParseImplicitAndWithDotNotation() {
        // { "address.city": "Paris", "address.zip": { "$lte": 75020 } }
        Document query = new Document("address.city", "Paris")
                .append("address.zip", new Document("$lte", 75020));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());

        EqualsOperation<?> eq = (EqualsOperation<?>) and.getConditions().get(0);
        assertEquals("address.city", eq.getFieldPath());
        assertEquals("Paris", eq.getValue());

        LessThanEqualsOperation<?> lte = (LessThanEqualsOperation<?>) and.getConditions().get(1);
        assertEquals("address.zip", lte.getFieldPath());
        assertEquals(75020, lte.getValue());
    }

    @Test
    void testParseOrWithDotNotation() {
        // { "$or": [ { "owner.name": "John" }, { "owner.age": { "$lt": 30 } } ] }
        Document query = new Document("$or", Arrays.asList(
                new Document("owner.name", "John"),
                new Document("owner.age", new Document("$lt", 30))));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof OrOperation);
        OrOperation or = (OrOperation) operation;
        assertEquals(2, or.getConditions().size());
        assertEquals("owner.name", ((EqualsOperation<?>) or.getConditions().get(0)).getFieldPath());
        assertEquals("owner.age", ((LessThanOperation<?>) or.getConditions().get(1)).getFieldPath());
    }

    @Test
    void testParseInWithDotNotation() {
        // { "specs.color": { "$in": [ "red", "blue" ] } }
        Document query = new Document("specs.color", new Document("$in", Arrays.asList("red", "blue")));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof InOperation);
        InOperation<?> in = (InOperation<?>) operation;
        assertEquals("specs.color", in.getFieldPath());
        assertEquals(Arrays.asList("red", "blue"), in.getValues());
    }

    @Test
    void testParseExistsWithDotNotation() {
        // { "profile.email": { "$exists": true } }
        Document query = new Document("profile.email", new Document("$exists", true));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ExistsOperation);
        ExistsOperation exists = (ExistsOperation) operation;
        assertEquals("profile.email", exists.getFieldPath());
        assertTrue(exists.getBoolean());
    }

    @Test
    void testParseSizeWithDotNotation() {
        // { "order.items": { "$size": 3 } }
        Document query = new Document("order.items", new Document("$size", 3));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof SizeOperation);
        SizeOperation size = (SizeOperation) operation;
        assertEquals("order.items", size.getFieldPath());
        assertEquals(3, size.getValue());
    }

    @Test
    void testParseNotWithDotNotation() {
        // { "stats.score": { "$not": { "$gt": 5 } } }
        Document query = new Document("stats.score", new Document("$not", new Document("$gt", 5)));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof NotOperation);
        NotOperation not = (NotOperation) operation;
        assertEquals("stats.score", not.getFieldPath());
        assertTrue(not.getCondition() instanceof GreaterThanOperation);
        GreaterThanOperation<?> gt = (GreaterThanOperation<?>) not.getCondition();
        assertEquals("stats.score", gt.getFieldPath());
        assertEquals(5, gt.getValue());
    }

    @Test
    void testParseElemMatchWithDotNotation() {
        // { "order.items": { "$elemMatch": { "qty": { "$gt": 2 } } } }
        Document query = new Document("order.items",
                new Document("$elemMatch", new Document("qty", new Document("$gt", 2))));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("order.items", elemMatch.getFieldPath());
        assertTrue(elemMatch.getCondition() instanceof GreaterThanOperation);
        assertEquals("qty", ((GreaterThanOperation<?>) elemMatch.getCondition()).getFieldPath());
    }

    @Test
    void testParseElemMatchWithDotNotationInsideCondition() {
        // { "items": { "$elemMatch": { "product.sku": "A1" } } }
        Document query = new Document("items",
                new Document("$elemMatch", new Document("product.sku", "A1")));
        QueryOperation operation = parser.parse(query);
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("items", elemMatch.getFieldPath());
        assertTrue(elemMatch.getCondition() instanceof EqualsOperation);
        EqualsOperation<?> eq = (EqualsOperation<?>) elemMatch.getCondition();
        assertEquals("product.sku", eq.getFieldPath());
        assertEquals("A1", eq.getValue());
    }

    @Test
    void testParseDotNotationFromFiltersBuilder() {
        Bson filter = Filters.and(
                Filters.eq("address.city", "Paris"),
                Filters.gt("items.0.price", 10));
        QueryOperation operation = parser.parse(convertToDocument(filter));
        assertTrue(operation instanceof AndOperation);
        AndOperation and = (AndOperation) operation;
        assertEquals(2, and.getConditions().size());
        assertEquals("address.city", ((EqualsOperation<?>) and.getConditions().get(0)).getFieldPath());
        assertEquals("items.0.price", ((GreaterThanOperation<?>) and.getConditions().get(1)).getFieldPath());
    }

    private ElemMatchOperation parseElemMatchCondition(
            Document condition,
            Class<? extends QueryOperation> expectedConditionType) {
        QueryOperation operation = parser.parse(
                new Document("tags", new Document("$elemMatch", condition))
        );
        assertTrue(operation instanceof ElemMatchOperation);
        ElemMatchOperation elemMatch = (ElemMatchOperation) operation;
        assertEquals("tags", elemMatch.getFieldPath());
        assertTrue(
                expectedConditionType.isInstance(elemMatch.getCondition()),
                () -> "Expected " + expectedConditionType.getSimpleName()
                        + " but got " + elemMatch.getCondition().getClass().getSimpleName()
        );
        return elemMatch;
    }

    private void assertInvalidQuery(Document query) {
        QueryOperation operation = assertDoesNotThrow(() -> parser.parse(query));
        assertNull(operation);
    }

    private Document convertToDocument(Bson filter) {
        DocumentCodec documentCodec = new DocumentCodec();
        return documentCodec.decode(filter.toBsonDocument().asBsonReader(), DecoderContext.builder().build());
    }
}
