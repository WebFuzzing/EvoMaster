package org.evomaster.client.java.controller.opensearch.utils;

import org.evomaster.client.java.controller.opensearch.operations.CommonQueryParameters;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParameterExtractor methods using actual OpenSearch query objects.
 */
class ParameterExtractorTest {

    @Test
    void testExtractCommonParameters_Term_AllFields() {
        Query query = Query.of(q -> q.term(t -> t
            .field("name")
            .value(FieldValue.of("value"))
            .boost(2.0f)
            .queryName("myQuery")
            .caseInsensitive(true)
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "term");
        
        assertNotNull(params);
        assertEquals(2.0f, params.getBoost());
        assertEquals("myQuery", params.getName());
        assertNull(params.getRewrite());
        assertTrue(params.getCaseInsensitive());
    }

    @Test
    void testExtractCommonParameters_Term_NullFields() {
        Query query = Query.of(q -> q.term(t -> t
            .field("name")
            .value(FieldValue.of("value"))
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "term");
        
        assertNotNull(params);
        assertNull(params.getBoost());
        assertNull(params.getName());
        assertNull(params.getRewrite());
        assertFalse(params.getCaseInsensitive());
    }

    @Test
    void testExtractCommonParameters_Term_PartialFields() {
        Query query = Query.of(q -> q.term(t -> t
            .field("name")
            .value(FieldValue.of("value"))
            .boost(1.5f)
            .caseInsensitive(false)
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "term");
        
        assertNotNull(params);
        assertEquals(1.5f, params.getBoost());
        assertNull(params.getName());
        assertNull(params.getRewrite());
        assertFalse(params.getCaseInsensitive());
    }

    @Test
    void testExtractCommonParameters_Prefix_WithRewrite() {
        Query query = Query.of(q -> q.prefix(p -> p
            .field("name")
            .value("test")
            .boost(1.2f)
            .queryName("user_search")
            .rewrite("constant_score")
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "prefix");
        
        assertNotNull(params);
        assertEquals(1.2f, params.getBoost());
        assertEquals("user_search", params.getName());
        assertEquals("constant_score", params.getRewrite());
    }

    @Test
    void testExtractCommonParameters_Wildcard_CaseInsensitive() {
        Query query = Query.of(q -> q.wildcard(w -> w
            .field("pattern")
            .value("test*")
            .caseInsensitive(true)
            .boost(2.0f)
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "wildcard");
        
        assertNotNull(params);
        assertEquals(2.0f, params.getBoost());
        assertTrue(params.getCaseInsensitive());
    }

    @Test
    void testExtractFieldValueParams_Term_StringValue() {
        Query query = Query.of(q -> q.term(t -> t
            .field("username")
            .value(FieldValue.of("john_doe"))
            .boost(1.2f)
            .queryName("user_search")
        ));
        
        ParameterExtractor.FieldValueParams params = ParameterExtractor.extractFieldValueParams(query, "term");
        
        assertNotNull(params);
        assertEquals("username", params.fieldName);
        assertEquals("john_doe", params.value);
        assertNotNull(params.commonParams);
        assertEquals(1.2f, params.commonParams.getBoost());
    }

    @Test
    void testExtractFieldValueParams_Term_EmptyValue() {
        Query query = Query.of(q -> q.term(t -> t
            .field("description")
            .value(FieldValue.of(""))
        ));
        
        ParameterExtractor.FieldValueParams params = ParameterExtractor.extractFieldValueParams(query, "term");
        
        assertNotNull(params);
        assertEquals("description", params.fieldName);
        assertEquals("", params.value);
    }

    @Test
    void testExtractCommonParameters_Prefix() {
        Query query = Query.of(q -> q.prefix(p -> p
            .field("name")
            .value("test")
            .boost(2.0f)
            .caseInsensitive(true)
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "prefix");
        
        assertNotNull(params);
        assertEquals(2.0f, params.getBoost());
        assertTrue(params.getCaseInsensitive());
    }

    @Test
    void testExtractCommonParameters_Wildcard() {
        Query query = Query.of(q -> q.wildcard(w -> w
            .field("pattern")
            .value("test*")
            .caseInsensitive(false)
            .boost(1.5f)
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "wildcard");
        
        assertNotNull(params);
        assertEquals(1.5f, params.getBoost());
        assertFalse(params.getCaseInsensitive());
    }

    @Test
    void testExtractCommonParameters_Regexp() {
        Query query = Query.of(q -> q.regexp(r -> r
            .field("email")
            .value(".*@test\\.com")
            .caseInsensitive(true)
        ));
        
        CommonQueryParameters params = ParameterExtractor.extractCommonParameters(query, "regexp");
        
        assertNotNull(params);
        assertTrue(params.getCaseInsensitive());
    }

    @Test
    void testFieldValueParams_Constructor() {
        CommonQueryParameters common = CommonQueryParameters.withBoost(2.5f);
        ParameterExtractor.FieldValueParams params = new ParameterExtractor.FieldValueParams("field", "value", common);
        
        assertEquals("field", params.fieldName);
        assertEquals("value", params.value);
        assertEquals(2.5f, params.commonParams.getBoost());
    }

    @Test
    void testFuzzyParams_Constructor() {
        ParameterExtractor.FieldValueParams baseParams = new ParameterExtractor.FieldValueParams(
            "field", "value", CommonQueryParameters.empty()
        );
        ParameterExtractor.FuzzyParams params = new ParameterExtractor.FuzzyParams(baseParams, 2, 50, 1, true);
        
        assertEquals("field", params.baseParams.fieldName);
        assertEquals(2, params.fuzziness);
        assertEquals(50, params.maxExpansions);
        assertEquals(1, params.prefixLength);
        assertTrue(params.transpositions);
    }

    @Test
    void testRegexpParams_Constructor() {
        ParameterExtractor.FieldValueParams baseParams = new ParameterExtractor.FieldValueParams(
            "pattern", ".*test", CommonQueryParameters.empty()
        );
        ParameterExtractor.RegexpParams params = new ParameterExtractor.RegexpParams(baseParams, "ALL", 10000);
        
        assertEquals("pattern", params.baseParams.fieldName);
        assertEquals("ALL", params.flags);
        assertEquals(10000, params.maxDeterminizedStates);
    }

    @Test
    void testMatchParams_Constructor() {
        ParameterExtractor.FieldValueParams baseParams = new ParameterExtractor.FieldValueParams(
            "content", "search", CommonQueryParameters.empty()
        );
        ParameterExtractor.MatchParams params = new ParameterExtractor.MatchParams(
            baseParams, "and", 2, "AUTO", 1, 50, "standard", true, false, false
        );
        
        assertEquals("content", params.baseParams.fieldName);
        assertEquals("and", params.operator);
        assertEquals(2, params.minimumShouldMatch);
        assertEquals("AUTO", params.fuzziness);
    }

    @Test
    void testRangeParams_Constructor() {
        ParameterExtractor.RangeParams params = new ParameterExtractor.RangeParams(
            "age", 18, 20, 65, 70, "yyyy-MM-dd", "INTERSECTS", 1.5f, "UTC"
        );
        
        assertEquals("age", params.fieldName);
        assertEquals(18, params.gte);
        assertEquals(20, params.gt);
        assertEquals(65, params.lte);
        assertEquals(70, params.lt);
        assertEquals("yyyy-MM-dd", params.format);
        assertEquals("INTERSECTS", params.relation);
        assertEquals(1.5f, params.boost);
        assertEquals("UTC", params.timeZone);
    }

    @Test
    void testExtractStringParameter_Regexp_Flags() {
        Query query = Query.of(q -> q.regexp(r -> r
            .field("email")
            .value(".*@test\\.com")
            .flags("ALL")
        ));
        
        String flags = OpenSearchQueryHelper.extractStringParameter(query, "regexp", "flags");
        assertEquals("ALL", flags);
    }

    @Test
    void testExtractStringParameter_Prefix_Rewrite() {
        Query query = Query.of(q -> q.prefix(p -> p
            .field("name")
            .value("test")
            .rewrite("scoring_boolean")
        ));
        
        String rewrite = OpenSearchQueryHelper.extractStringParameter(query, "prefix", "rewrite");
        assertEquals("scoring_boolean", rewrite);
    }

    @Test
    void testExtractIntegerParameter_Regexp_MaxDeterminizedStates() {
        Query query = Query.of(q -> q.regexp(r -> r
            .field("pattern")
            .value("test.*")
            .maxDeterminizedStates(10000)
        ));
        
        Integer maxStates = OpenSearchQueryHelper.extractIntegerParameter(query, "regexp", "maxDeterminizedStates");
        assertEquals(10000, maxStates);
    }

    @Test
    void testExtractIntegerParameter_Wildcard_Null() {
        Query query = Query.of(q -> q.wildcard(w -> w
            .field("pattern")
            .value("test*")
        ));
        
        Integer maxExpansions = OpenSearchQueryHelper.extractIntegerParameter(query, "wildcard", "maxExpansions");
        assertNull(maxExpansions);
    }
}
