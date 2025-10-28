package org.evomaster.client.java.controller.opensearch;

import org.evomaster.client.java.controller.opensearch.operations.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenSearchHeuristicsCalculator methods.
 */
class OpenSearchHeuristicsCalculatorDirectTest {

    private final OpenSearchHeuristicsCalculator calculator = new OpenSearchHeuristicsCalculator();

    // Test data creation helpers
    private Map<String, Object> createDoc(String field, Object value) {
        Map<String, Object> doc = new HashMap<>();
        doc.put(field, value);
        return doc;
    }

    private Map<String, Object> createDoc(Map<String, Object> fields) {
        return new HashMap<>(fields);
    }

    // ===== Term Operation Tests =====

    @Test
    void testCalculateDistanceForEquals() {
        TermOperation<String> operation = new TermOperation<>("name", "john");
        Map<String, Object> docMatch = createDoc("name", "john");
        Map<String, Object> docNoMatch = createDoc("name", "jane");
        Map<String, Object> docMissing = createDoc("age", 25);
        
        double distanceMatch = calculator.calculateDistance(operation, docMatch);
        double distanceNoMatch = calculator.calculateDistance(operation, docNoMatch);
        double distanceMissing = calculator.calculateDistance(operation, docMissing);
        
        assertEquals(0.0, distanceMatch, "Exact match should return 0 distance");
        assertTrue(distanceNoMatch > 0, "Different values should return positive distance");
        assertEquals(Double.MAX_VALUE, distanceMissing, "Missing field should return MAX_VALUE");
    }

    @Test
    void testCalculateDistanceForEqualsNumeric() {
        TermOperation<Integer> operation = new TermOperation<>("age", 25);
        Map<String, Object> doc = createDoc("age", 30);
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(5.0, distance, "Numeric difference should be absolute value");
    }

    @Test
    void testCalculateDistanceForEqualsCaseInsensitive() {
        TermOperation<String> operation = new TermOperation<>("name", "JOHN", true);
        Map<String, Object> doc = createDoc("name", "john");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "Case insensitive match should return 0 distance");
    }

    // ===== Terms Operation Tests =====

    @Test
    void testCalculateDistanceForTerms() {
        TermsOperation<String> operation = new TermsOperation<>("category", Arrays.asList("sports", "music", "tech"));
        Map<String, Object> docMatch = createDoc("category", "sports");
        Map<String, Object> docNoMatch = createDoc("category", "science");
        Map<String, Object> docMissing = createDoc("age", 25);
        
        double distanceMatch = calculator.calculateDistance(operation, docMatch);
        double distanceNoMatch = calculator.calculateDistance(operation, docNoMatch);
        double distanceMissing = calculator.calculateDistance(operation, docMissing);
        
        assertEquals(0.0, distanceMatch, "Matching any term should return 0 distance");
        assertTrue(distanceNoMatch > 0, "No matching terms should return positive distance");
        assertEquals(Double.MAX_VALUE, distanceMissing, "Missing field should return MAX_VALUE");
    }

    // ===== TermsSet Operation Tests =====

    @Test
    void testCalculateDistanceForTermsSet() {
        TermsSetOperation<String> operation = new TermsSetOperation<>("tags", 
            Arrays.asList("java", "spring", "database"), "min_match");
        Map<String, Object> docSatisfied = new HashMap<>();
        docSatisfied.put("tags", "java");
        docSatisfied.put("min_match", 1);
        
        Map<String, Object> docNotSatisfied = new HashMap<>();
        docNotSatisfied.put("tags", "python");
        docNotSatisfied.put("min_match", 2);
        
        double distanceSatisfied = calculator.calculateDistance(operation, docSatisfied);
        double distanceNotSatisfied = calculator.calculateDistance(operation, docNotSatisfied);
        
        assertEquals(0.0, distanceSatisfied, "Meeting minimum requirement should return 0 distance");
        assertTrue(distanceNotSatisfied > 0, "Not meeting minimum requirement should return positive distance");
    }

    // ===== Range Operation Tests =====

    @Test
    void testCalculateDistanceForRangeGte() {
        RangeOperation operation = new RangeOperation("age", 18, null, null, null);
        Map<String, Object> docWithin = createDoc("age", 25);
        Map<String, Object> docViolation = createDoc("age", 15);
        
        double distanceWithin = calculator.calculateDistance(operation, docWithin);
        double distanceViolation = calculator.calculateDistance(operation, docViolation);
        
        assertEquals(0.0, distanceWithin, "Value >= lower bound should return 0 distance");
        assertEquals(3.0, distanceViolation, "Value < lower bound should return positive distance");
    }

    @Test
    void testCalculateDistanceForRangeGt() {
        RangeOperation operation = new RangeOperation("score", null, 50, null, null);
        Map<String, Object> docWithin = createDoc("score", 60);
        Map<String, Object> docViolation = createDoc("score", 50);
        
        double distanceWithin = calculator.calculateDistance(operation, docWithin);
        double distanceViolation = calculator.calculateDistance(operation, docViolation);
        
        assertEquals(0.0, distanceWithin, "Value > lower bound should return 0 distance");
        assertEquals(1.0, distanceViolation, "Value <= lower bound should return positive distance");
    }

    @Test
    void testCalculateDistanceForRangeLte() {
        RangeOperation operation = new RangeOperation("price", null, null, 100, null);
        Map<String, Object> docWithin = createDoc("price", 80);
        Map<String, Object> docViolation = createDoc("price", 120);
        
        double distanceWithin = calculator.calculateDistance(operation, docWithin);
        double distanceViolation = calculator.calculateDistance(operation, docViolation);
        
        assertEquals(0.0, distanceWithin, "Value <= upper bound should return 0 distance");
        assertEquals(20.0, distanceViolation, "Value > upper bound should return positive distance");
    }

    @Test
    void testCalculateDistanceForRangeLt() {
        RangeOperation operation = new RangeOperation("temperature", null, null, null, 100);
        Map<String, Object> docWithin = createDoc("temperature", 80);
        Map<String, Object> docViolation = createDoc("temperature", 100);
        
        double distanceWithin = calculator.calculateDistance(operation, docWithin);
        double distanceViolation = calculator.calculateDistance(operation, docViolation);
        
        assertEquals(0.0, distanceWithin, "Value < upper bound should return 0 distance");
        assertEquals(1.0, distanceViolation, "Value >= upper bound should return positive distance");
    }

    @Test
    void testCalculateDistanceForRangeMissingField() {
        RangeOperation operation = new RangeOperation("age", 18, null, null, null);
        Map<String, Object> doc = createDoc("name", "john");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(Double.MAX_VALUE, distance, "Missing field should return MAX_VALUE");
    }

    // ===== Prefix Operation Tests =====

    @Test
    void testCalculateDistanceForPrefix() {
        PrefixOperation operation = new PrefixOperation("title", "hello");
        Map<String, Object> docMatch = createDoc("title", "hello world");
        Map<String, Object> docNoMatch = createDoc("title", "goodbye world");
        
        double distanceMatch = calculator.calculateDistance(operation, docMatch);
        double distanceNoMatch = calculator.calculateDistance(operation, docNoMatch);
        
        assertEquals(0.0, distanceMatch, "Matching prefix should return 0 distance");
        assertTrue(distanceNoMatch > 0, "Non-matching prefix should return positive distance");
    }

    @Test
    void testCalculateDistanceForPrefixCaseInsensitive() {
        PrefixOperation operation = new PrefixOperation("title", "HELLO", CommonQueryParameters.withCaseInsensitive(true));
        Map<String, Object> doc = createDoc("title", "hello world");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "Case insensitive prefix match should return 0 distance");
    }

    // ===== Exists Operation Tests =====

    @Test
    void testCalculateDistanceForExists() {
        ExistsOperation operation = new ExistsOperation("name");
        Map<String, Object> docExists = createDoc("name", "john");
        Map<String, Object> docMissing = createDoc("age", 25);
        Map<String, Object> docNull = createDoc("name", null);
        
        double distanceExists = calculator.calculateDistance(operation, docExists);
        double distanceMissing = calculator.calculateDistance(operation, docMissing);
        double distanceNull = calculator.calculateDistance(operation, docNull);
        
        assertEquals(0.0, distanceExists, "Existing field should return 0 distance");
        assertEquals(1.0, distanceMissing, "Missing field should return 1.0 distance");
        assertEquals(1.0, distanceNull, "Null field should return 1.0 distance");
    }

    // ===== IDs Operation Tests =====

    @Test
    void testCalculateDistanceForIds() {
        IdsOperation operation = new IdsOperation(Arrays.asList("doc1", "doc2", "doc3"));
        Map<String, Object> docMatch = createDoc("_id", "doc2");
        Map<String, Object> docNoMatch = createDoc("_id", "doc5");
        Map<String, Object> docMissing = createDoc("name", "john");
        
        double distanceMatch = calculator.calculateDistance(operation, docMatch);
        double distanceNoMatch = calculator.calculateDistance(operation, docNoMatch);
        double distanceMissing = calculator.calculateDistance(operation, docMissing);
        
        assertEquals(0.0, distanceMatch, "Matching ID should return 0 distance");
        assertTrue(distanceNoMatch > 0, "Non-matching ID should return positive distance");
        assertEquals(Double.MAX_VALUE, distanceMissing, "Missing _id field should return MAX_VALUE");
    }

    // ===== Fuzzy Operation Tests =====

    @Test
    void testCalculateDistanceForFuzzy() {
        FuzzyOperation operation = new FuzzyOperation("name", "john", null, 2, null, null, true, null);
        Map<String, Object> docExact = createDoc("name", "john");
        Map<String, Object> docWithin = createDoc("name", "johnn"); // 1 edit distance
        Map<String, Object> docExceeds = createDoc("name", "jones"); // > 2 edit distance (likely)
        
        double distanceExact = calculator.calculateDistance(operation, docExact);
        double distanceWithin = calculator.calculateDistance(operation, docWithin);
        double distanceExceeds = calculator.calculateDistance(operation, docExceeds);
        
        assertEquals(0.0, distanceExact, "Exact match should return 0 distance");
        assertEquals(0.0, distanceWithin, "Within fuzziness should return 0 distance");
        assertTrue(distanceExceeds > 0, "Exceeding fuzziness should return positive distance");
    }

    // ===== Wildcard Operation Tests =====

    @Test
    void testCalculateDistanceForWildcard() {
        WildcardOperation operation = new WildcardOperation("title", "hel*world");
        Map<String, Object> docMatch = createDoc("title", "hello world");
        Map<String, Object> docNoMatch = createDoc("title", "goodbye earth");
        
        double distanceMatch = calculator.calculateDistance(operation, docMatch);
        double distanceNoMatch = calculator.calculateDistance(operation, docNoMatch);
        
        assertEquals(0.0, distanceMatch, "Matching wildcard should return 0 distance");
        assertTrue(distanceNoMatch > 0, "Non-matching wildcard should return positive distance");
    }

    // ===== Regexp Operation Tests =====

    @Test
    void testCalculateDistanceForRegexp() {
        RegexpOperation operation = new RegexpOperation("email", ".*@gmail\\.com", null, false, null, null, null);
        Map<String, Object> docMatch = createDoc("email", "user@gmail.com");
        Map<String, Object> docNoMatch = createDoc("email", "user@yahoo.com");
        
        double distanceMatch = calculator.calculateDistance(operation, docMatch);
        double distanceNoMatch = calculator.calculateDistance(operation, docNoMatch);
        
        assertEquals(0.0, distanceMatch, "Matching regex should return 0 distance");
        assertTrue(distanceNoMatch > 0, "Non-matching regex should return positive distance");
    }

    // ===== Match Operation Tests =====

    @Test
    void testCalculateDistanceForMatchSingle() {
        MatchOperation operation = new MatchOperation("content", "java", CommonQueryParameters.empty(), "or", null, null, null, null, null, null, null, null, null);
        Map<String, Object> doc = createDoc("content", "I love java programming");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "Matching token should return 0 distance");
    }

    @Test
    void testCalculateDistanceForMatchAnd() {
        MatchOperation operation = new MatchOperation("content", "java programming", CommonQueryParameters.empty(), "and", null, null, null, null, null, null, null, null, null);
        Map<String, Object> doc = createDoc("content", "I love java and programming");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "All tokens matching should return 0 distance");
    }

    @Test
    void testCalculateDistanceForMatchOr() {
        MatchOperation operation = new MatchOperation("content", "java python", CommonQueryParameters.empty(), "or", null, null, null, null, null, null, null, null, null);
        Map<String, Object> doc = createDoc("content", "I love java programming");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "Any token matching should return 0 distance");
    }

    // ===== Bool Operation Tests =====

    @Test
    void testCalculateDistanceForBoolMust() {
        TermOperation<String> term1 = new TermOperation<>("category", "tech");
        TermOperation<Integer> term2 = new TermOperation<>("score", 85);
        BoolOperation operation = new BoolOperation(
            Arrays.asList(term1, term2), // must
            null, // must_not
            null, // should
            null  // filter
        );
        
        Map<String, Object> doc = new HashMap<>();
        doc.put("category", "tech");
        doc.put("score", 85);
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "All must clauses matching should return 0 distance");
    }

    @Test
    void testCalculateDistanceForBoolMustNot() {
        TermOperation<String> term1 = new TermOperation<>("status", "deleted");
        BoolOperation operation = new BoolOperation(
            null, // must
            Arrays.asList(term1), // must_not
            null, // should
            null  // filter
        );
        
        Map<String, Object> docGood = createDoc("status", "active");
        Map<String, Object> docBad = createDoc("status", "deleted");
        
        double distanceGood = calculator.calculateDistance(operation, docGood);
        double distanceBad = calculator.calculateDistance(operation, docBad);
        
        assertEquals(0.0, distanceGood, "Must_not clause not matching should return 0 distance");
        assertEquals(Double.MAX_VALUE, distanceBad, "Must_not clause matching should return MAX_VALUE");
    }

    @Test
    void testCalculateDistanceForBoolShould() {
        TermOperation<String> term1 = new TermOperation<>("category", "tech");
        TermOperation<String> term2 = new TermOperation<>("category", "science");
        BoolOperation operation = new BoolOperation(
            null, // must
            null, // must_not
            Arrays.asList(term1, term2), // should
            null, // filter
            1,    // minimum_should_match
            null  // boost
        );
        
        Map<String, Object> doc = createDoc("category", "tech");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(0.0, distance, "Meeting minimum_should_match should return 0 distance");
    }

    // ===== Edge Cases and Error Handling =====

    @Test
    void testUnknownOperationType() {
        QueryOperation unknownOperation = new QueryOperation() {};
        Map<String, Object> doc = createDoc("field", "value");
        
        double distance = calculator.calculateDistance(unknownOperation, doc);
        assertEquals(Double.MAX_VALUE, distance, "Unknown operation type should return MAX_VALUE");
    }

    @Test
    void testRangeOperationWithStringValue() {
        RangeOperation operation = new RangeOperation("age", 18, null, null, null);
        Map<String, Object> doc = createDoc("age", "twenty-five");
        
        double distance = calculator.calculateDistance(operation, doc);
        assertEquals(Double.MAX_VALUE, distance, "Non-numeric value in range operation should return MAX_VALUE");
    }
}
