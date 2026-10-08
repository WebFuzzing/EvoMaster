package org.evomaster.client.java.controller.mongo;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldPathResolverTest {

    /*
        ================================================================================
        getActualValues
        ================================================================================
     */

    @Test
    void testTopLevelField() {
        Document doc = new Document("a", 1);
        assertEquals(Collections.singletonList(1), FieldPathResolver.getActualValues(doc, "a"));
    }

    @Test
    void testMissingTopLevelFieldIsNull() {
        Document doc = new Document("a", 1);
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "b"));
    }

    @Test
    void testTopLevelFieldHoldingNull() {
        Document doc = new Document("a", null);
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a"));
    }

    @Test
    void testEmptyDocument() {
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(new Document(), "a"));
    }

    @Test
    void testTopLevelFieldHoldingArrayIsReturnedAsIs() {
        List<Integer> array = Arrays.asList(1, 2, 3);
        Document doc = new Document("a", array);
        assertEquals(Collections.singletonList(array), FieldPathResolver.getActualValues(doc, "a"));
    }

    @Test
    void testTopLevelFieldHoldingSubDocumentIsReturnedAsIs() {
        Document inner = new Document("b", 1);
        Document doc = new Document("a", inner);
        assertEquals(Collections.singletonList(inner), FieldPathResolver.getActualValues(doc, "a"));
    }

    @Test
    void testNestedField() {
        Document doc = new Document("a", new Document("b", 1));
        assertEquals(Collections.singletonList(1), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testDeeplyNestedField() {
        Document doc = new Document("a", new Document("b", new Document("c", new Document("d", "x"))));
        assertEquals(Collections.singletonList("x"), FieldPathResolver.getActualValues(doc, "a.b.c.d"));
    }

    @Test
    void testMissingIntermediateField() {
        Document doc = new Document("x", new Document("b", 1));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testMissingLeafField() {
        Document doc = new Document("a", new Document("c", 1));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testPathThroughScalar() {
        Document doc = new Document("a", 5);
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testPathThroughNull() {
        Document doc = new Document("a", null);
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testDottedPathIsNotALiteralKey() {
        // a query path "a.b" does not address a top-level field literally named "a.b"
        Document doc = new Document("a.b", 1);
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testArrayIndex() {
        Document doc = new Document("a", Arrays.asList(10, 20, 30));
        assertEquals(Collections.singletonList(10), FieldPathResolver.getActualValues(doc, "a.0"));
        assertEquals(Collections.singletonList(20), FieldPathResolver.getActualValues(doc, "a.1"));
        assertEquals(Collections.singletonList(30), FieldPathResolver.getActualValues(doc, "a.2"));
    }

    @Test
    void testArrayIndexOutOfBounds() {
        Document doc = new Document("a", Arrays.asList(10, 20, 30));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.3"));
    }

    @Test
    void testArrayIndexOnEmptyArray() {
        Document doc = new Document("a", Collections.emptyList());
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.0"));
    }

    @Test
    void testNonNumericSegmentOnArrayOfScalars() {
        Document doc = new Document("a", Arrays.asList(1, 2, 3));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testNegativeIndexIsNotAnArrayIndex() {
        Document doc = new Document("a", Arrays.asList(1, 2, 3));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.-1"));
    }

    @Test
    void testFieldOfEachSubDocumentInArray() {
        Document doc = new Document("a", Arrays.asList(
                new Document("b", 1),
                new Document("b", 2)));
        assertEquals(Arrays.asList(1, 2), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testFieldMissingInSomeSubDocumentsOfArray() {
        Document doc = new Document("a", Arrays.asList(
                new Document("b", 1),
                new Document("c", 2)));
        assertEquals(Arrays.asList(1, null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testArrayMixingScalarsAndSubDocuments() {
        // scalar elements are not traversed, only sub-documents are
        Document doc = new Document("a", Arrays.asList(
                5,
                new Document("b", 1)));
        assertEquals(Collections.singletonList(1), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testArrayIndexFollowedBySubField() {
        Document doc = new Document("items", Arrays.asList(
                new Document("price", 5),
                new Document("price", 15)));
        List<Object> values = FieldPathResolver.getActualValues(doc, "items.1.price");
        assertTrue(values.contains(15));
        assertFalse(values.contains(5));
    }

    @Test
    void testNumericSegmentAlsoMatchesFieldOfSubDocuments() {
        // "0" is both an index into the array and a field name of its sub-documents
        Document doc = new Document("a", Arrays.asList(
                new Document("0", "field"),
                new Document("x", 1)));
        List<Object> values = FieldPathResolver.getActualValues(doc, "a.0");
        assertTrue(values.contains(new Document("0", "field")));
        assertTrue(values.contains("field"));
    }

    @Test
    void testPathReachingAnArrayInsideAnArrayOfSubDocuments() {
        List<Integer> inner = Arrays.asList(1, 2);
        Document doc = new Document("a", Arrays.asList(
                new Document("b", inner),
                new Document("b", 3)));
        assertEquals(Arrays.asList(inner, 3), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testNestedArraysAreNotImplicitlyTraversed() {
        Document doc = new Document("a", Collections.singletonList(
                Collections.singletonList(new Document("b", 1))));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(doc, "a.b"));
    }

    @Test
    void testNestedArraysTraversedWithExplicitIndexes() {
        Document doc = new Document("a", Collections.singletonList(
                Collections.singletonList(new Document("b", 1))));
        assertTrue(FieldPathResolver.getActualValues(doc, "a.0.0.b").contains(1));
    }

    @Test
    void testMultipleLevelsOfArraysOfSubDocuments() {
        Document doc = new Document("a", Arrays.asList(
                new Document("b", Arrays.asList(new Document("c", 1), new Document("c", 2))),
                new Document("b", Collections.singletonList(new Document("c", 3)))));
        assertEquals(Arrays.asList(1, 2, 3), FieldPathResolver.getActualValues(doc, "a.b.c"));
    }

    @Test
    void testNonDocumentRoot() {
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues(5, "a"));
        assertEquals(Collections.singletonList(null), FieldPathResolver.getActualValues("hello", "a.b"));
    }

    @Test
    void testResultIsNeverEmpty() {
        Document doc = new Document("a", Collections.emptyList());
        assertFalse(FieldPathResolver.getActualValues(doc, "a.b").isEmpty());
        assertFalse(FieldPathResolver.getActualValues(new Document(), "x.y.z").isEmpty());
    }

    /*
        ================================================================================
        isFieldPathPresent
        ================================================================================
     */

    @Test
    void testTopLevelFieldIsPresent() {
        Document doc = new Document("a", 1);
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "b"));
    }

    @Test
    void testFieldHoldingNullIsPresent() {
        // a field holding null exists, unlike a missing field
        Document doc = new Document("a", null);
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a"));
    }

    @Test
    void testNestedFieldIsPresent() {
        Document doc = new Document("a", new Document("b", new Document("c", 1)));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a"));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.b"));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.b.c"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.b.d"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.x.c"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.b.c.d"));
    }

    @Test
    void testPathThroughScalarIsNotPresent() {
        Document doc = new Document("a", 5);
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.b"));
    }

    @Test
    void testArrayIndexIsPresent() {
        Document doc = new Document("a", Arrays.asList(10, 20));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.0"));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.1"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.2"));
    }

    @Test
    void testFieldIsPresentInSomeSubDocumentOfArray() {
        Document doc = new Document("a", Arrays.asList(
                new Document("x", 1),
                new Document("b", 2)));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.b"));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.1.b"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.0.b"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.c"));
    }

    @Test
    void testFieldInsideNestedArrayIsNotPresent() {
        Document doc = new Document("a", Collections.singletonList(
                Collections.singletonList(new Document("b", 1))));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.b"));
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a.0.0.b"));
    }

    @Test
    void testFieldIsNotPresentInEmptyArray() {
        Document doc = new Document("a", Collections.emptyList());
        assertTrue(FieldPathResolver.isFieldPathPresent(doc, "a"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.b"));
        assertFalse(FieldPathResolver.isFieldPathPresent(doc, "a.0"));
    }

    @Test
    void testFieldIsNotPresentInNonDocumentRoot() {
        assertFalse(FieldPathResolver.isFieldPathPresent(5, "a"));
    }

    /*
        ================================================================================
        splitFieldPath
        ================================================================================
     */

    @Test
    void testSplitSingleSegment() {
        assertArrayEquals(new String[]{"a"}, FieldPathResolver.splitFieldPath("a"));
    }

    @Test
    void testSplitMultipleSegments() {
        assertArrayEquals(new String[]{"a", "b", "0", "c"}, FieldPathResolver.splitFieldPath("a.b.0.c"));
    }

    @Test
    void testSplitKeepsEmptySegments() {
        assertArrayEquals(new String[]{"a", ""}, FieldPathResolver.splitFieldPath("a."));
        assertArrayEquals(new String[]{"", "a"}, FieldPathResolver.splitFieldPath(".a"));
        assertArrayEquals(new String[]{"a", "", "b"}, FieldPathResolver.splitFieldPath("a..b"));
    }

    @Test
    void testSplitTreatsSeparatorLiterally() {
        // the separator is not interpreted as a regex wildcard
        assertArrayEquals(new String[]{"ab"}, FieldPathResolver.splitFieldPath("ab"));
    }

    /*
        ================================================================================
        toArrayIndex
        ================================================================================
     */

    @Test
    void testToArrayIndexValid() {
        assertEquals(0, FieldPathResolver.parseAsArrayIndex("0").getAsInt());
        assertEquals(7, FieldPathResolver.parseAsArrayIndex("7").getAsInt());
        assertEquals(42, FieldPathResolver.parseAsArrayIndex("42").getAsInt());
        assertEquals(1, FieldPathResolver.parseAsArrayIndex("01").getAsInt());
    }

    @Test
    void testToArrayIndexInvalid() {
        assertFalse(FieldPathResolver.parseAsArrayIndex("").isPresent());
        assertFalse(FieldPathResolver.parseAsArrayIndex("a").isPresent());
        assertFalse(FieldPathResolver.parseAsArrayIndex("1a").isPresent());
        assertFalse(FieldPathResolver.parseAsArrayIndex("-1").isPresent());
        assertFalse(FieldPathResolver.parseAsArrayIndex("+1").isPresent());
        assertFalse(FieldPathResolver.parseAsArrayIndex("1.5").isPresent());
        assertFalse(FieldPathResolver.parseAsArrayIndex(" 1").isPresent());
    }

    @Test
    void testToArrayIndexOverflow() {
        assertFalse(FieldPathResolver.parseAsArrayIndex("99999999999999999999").isPresent());
    }
}
