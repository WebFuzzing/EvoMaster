package org.evomaster.client.java.controller.opensearch.utils;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.json.JsonData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenSearchQueryHelper methods using actual OpenSearch query objects.
 */
class OpenSearchQueryHelperTest {
    @Test
    void testExtractQueryKind_Term() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("test"))));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Term", kind);
    }

    @Test
    void testExtractQueryKind_Match() {
        Query query = Query.of(q -> q.match(m -> m.field("description").query(FieldValue.of("hello"))));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Match", kind);
    }

    @Test
    void testExtractQueryKind_Range() {
        Query query = Query.of(q -> q.range(r -> r.field("age").gte(JsonData.of(18))));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Range", kind);
    }

    @Test
    void testExtractQueryKind_Prefix() {
        Query query = Query.of(q -> q.prefix(p -> p.field("name").value("test")));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Prefix", kind);
    }

    @Test
    void testExtractQueryKind_Exists() {
        Query query = Query.of(q -> q.exists(e -> e.field("username")));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Exists", kind);
    }

    @Test
    void testExtractQueryKind_Fuzzy() {
        Query query = Query.of(q -> q.fuzzy(f -> f.field("name").value(FieldValue.of("test"))));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Fuzzy", kind);
    }

    @Test
    void testExtractQueryKind_Wildcard() {
        Query query = Query.of(q -> q.wildcard(w -> w.field("pattern").value("test*")));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Wildcard", kind);
    }

    @Test
    void testExtractQueryKind_Regexp() {
        Query query = Query.of(q -> q.regexp(r -> r.field("email").value(".*@test\\.com")));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Regexp", kind);
    }

    @Test
    void testExtractQueryKind_Bool() {
        Query query = Query.of(q -> q.bool(b -> b
            .must(Query.of(mq -> mq.term(t -> t.field("status").value(FieldValue.of("active")))))
        ));
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Bool", kind);
    }

    @Test
    void testExtractFieldName_Term() {
        Query query = Query.of(q -> q.term(t -> t.field("username").value(FieldValue.of("john"))));
        String fieldName = OpenSearchQueryHelper.extractFieldName(query, "term");
        assertEquals("username", fieldName);
    }

    @Test
    void testExtractFieldName_DifferentFields() {
        Query query1 = Query.of(q -> q.term(t -> t.field("email").value(FieldValue.of("test@example.com"))));
        Query query2 = Query.of(q -> q.term(t -> t.field("age").value(FieldValue.of(25))));
        
        assertEquals("email", OpenSearchQueryHelper.extractFieldName(query1, "term"));
        assertEquals("age", OpenSearchQueryHelper.extractFieldName(query2, "term"));
    }

    @Test
    void testExtractFieldName_Range() {
        Query query = Query.of(q -> q.range(r -> r.field("price").gte(JsonData.of(100))));
        String fieldName = OpenSearchQueryHelper.extractFieldName(query, "range");
        assertEquals("price", fieldName);
    }

    @Test
    void testExtractFieldName_Prefix() {
        Query query = Query.of(q -> q.prefix(p -> p.field("title").value("hello")));
        String fieldName = OpenSearchQueryHelper.extractFieldName(query, "prefix");
        assertEquals("title", fieldName);
    }

    @Test
    void testExtractFieldValue_String() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("Alice"))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals("Alice", value);
    }

    @Test
    void testExtractFieldValue_Long() {
        Query query = Query.of(q -> q.term(t -> t.field("count").value(FieldValue.of(42L))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals(42L, value);
    }

    @Test
    void testExtractFieldValue_Double() {
        Query query = Query.of(q -> q.term(t -> t.field("price").value(FieldValue.of(99.99))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals(99.99, value);
    }

    @Test
    void testExtractFieldValue_Boolean() {
        Query query = Query.of(q -> q.term(t -> t.field("active").value(FieldValue.of(true))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals(true, value);
    }

    @Test
    void testExtractCaseInsensitive_True() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("john")).caseInsensitive(true)));
        Boolean caseInsensitive = OpenSearchQueryHelper.extractCaseInsensitive(query, "term");
        assertTrue(caseInsensitive);
    }

    @Test
    void testExtractCaseInsensitive_False() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("john")).caseInsensitive(false)));
        Boolean caseInsensitive = OpenSearchQueryHelper.extractCaseInsensitive(query, "term");
        assertFalse(caseInsensitive);
    }

    @Test
    void testExtractCaseInsensitive_Null_DefaultsFalse() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("john"))));
        Boolean caseInsensitive = OpenSearchQueryHelper.extractCaseInsensitive(query, "term");
        assertFalse(caseInsensitive);
    }

    @Test
    void testExtractBoost_WithValue() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("value")).boost(2.5f)));
        Float boost = OpenSearchQueryHelper.extractBoost(query, "term");
        assertEquals(2.5f, boost);
    }

    @Test
    void testExtractBoost_Null() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("value"))));
        Float boost = OpenSearchQueryHelper.extractBoost(query, "term");
        assertNull(boost);
    }

    @Test
    void testExtractQueryName_WithValue() {
        Query query = Query.of(q -> q.term(t -> t.field("field").value(FieldValue.of("value")).queryName("my_query_name")));
        String name = OpenSearchQueryHelper.extractQueryName(query, "term");
        assertEquals("my_query_name", name);
    }

    @Test
    void testExtractQueryName_Null() {
        Query query = Query.of(q -> q.term(t -> t.field("field").value(FieldValue.of("value"))));
        String name = OpenSearchQueryHelper.extractQueryName(query, "term");
        assertNull(name);
    }

    @Test
    void testExtractRewrite_Prefix_WithValue() {
        Query query = Query.of(q -> q.prefix(p -> p.field("field").value("value").rewrite("constant_score")));
        String rewrite = OpenSearchQueryHelper.extractRewrite(query, "prefix");
        assertEquals("constant_score", rewrite);
    }

    @Test
    void testExtractRewrite_Prefix_Null() {
        Query query = Query.of(q -> q.prefix(p -> p.field("field").value("value")));
        String rewrite = OpenSearchQueryHelper.extractRewrite(query, "prefix");
        assertNull(rewrite);
    }

    @Test
    void testExtractIdsValues_MultipleIds() {
        Query query = Query.of(q -> q.ids(i -> i.values(Arrays.asList("id1", "id2", "id3"))));
        List<String> ids = OpenSearchQueryHelper.extractIdsValues(query, "ids");
        
        assertNotNull(ids);
        assertEquals(3, ids.size());
        assertEquals("id1", ids.get(0));
        assertEquals("id2", ids.get(1));
        assertEquals("id3", ids.get(2));
    }

    @Test
    void testExtractIdsValues_SingleId() {
        Query query = Query.of(q -> q.ids(i -> i.values(Arrays.asList("doc123"))));
        List<String> ids = OpenSearchQueryHelper.extractIdsValues(query, "ids");
        
        assertNotNull(ids);
        assertEquals(1, ids.size());
        assertEquals("doc123", ids.get(0));
    }

    @Test
    void testExtractIdsValues_EmptyList() {
        Query query = Query.of(q -> q.ids(i -> i.values(new ArrayList<>())));
        List<String> ids = OpenSearchQueryHelper.extractIdsValues(query, "ids");
        
        assertNotNull(ids);
        assertTrue(ids.isEmpty());
    }

    @Test
    void testExtractExistsField() {
        Query query = Query.of(q -> q.exists(e -> e.field("username")));
        String field = OpenSearchQueryHelper.extractExistsField(query, "exists");
        assertEquals("username", field);
    }

    @Test
    void testExtractExistsField_DifferentFields() {
        Query query1 = Query.of(q -> q.exists(e -> e.field("email")));
        Query query2 = Query.of(q -> q.exists(e -> e.field("phone")));
        
        assertEquals("email", OpenSearchQueryHelper.extractExistsField(query1, "exists"));
        assertEquals("phone", OpenSearchQueryHelper.extractExistsField(query2, "exists"));
    }

    @Test
    void testExtractBooleanParameter_CaseInsensitive_True() {
        Query query = Query.of(q -> q.wildcard(w -> w.field("name").value("test*").caseInsensitive(true)));
        Boolean caseInsensitive = OpenSearchQueryHelper.extractBooleanParameter(query, "wildcard", "caseInsensitive");
        assertTrue(caseInsensitive);
    }

    @Test
    void testExtractBooleanParameter_CaseInsensitive_False() {
        Query query = Query.of(q -> q.wildcard(w -> w.field("name").value("test*").caseInsensitive(false)));
        Boolean caseInsensitive = OpenSearchQueryHelper.extractBooleanParameter(query, "wildcard", "caseInsensitive");
        assertFalse(caseInsensitive);
    }

    @Test
    void testExtractBoolClause_Must() {
        Query query = Query.of(q -> q.bool(b -> b
            .must(Query.of(mq -> mq.term(t -> t.field("status").value(FieldValue.of("active")))))
            .must(Query.of(mq -> mq.range(r -> r.field("age").gte(JsonData.of(18)))))
        ));
        
        List<Object> must = OpenSearchQueryHelper.extractBoolClause(query, "bool", "must");
        assertNotNull(must);
        assertEquals(2, must.size());
    }

    @Test
    void testExtractBoolClause_Should() {
        Query query = Query.of(q -> q.bool(b -> b
            .should(Query.of(sq -> sq.match(m -> m.field("title").query(FieldValue.of("test")))))
        ));
        
        List<Object> should = OpenSearchQueryHelper.extractBoolClause(query, "bool", "should");
        assertNotNull(should);
        assertEquals(1, should.size());
    }

    @Test
    void testExtractBoolClause_MustNot() {
        Query query = Query.of(q -> q.bool(b -> b
            .mustNot(Query.of(mnq -> mnq.term(t -> t.field("deleted").value(FieldValue.of(true)))))
            .mustNot(Query.of(mnq -> mnq.exists(e -> e.field("archived"))))
        ));
        
        List<Object> mustNot = OpenSearchQueryHelper.extractBoolClause(query, "bool", "mustNot");
        assertNotNull(mustNot);
        assertEquals(2, mustNot.size());
    }

    @Test
    void testExtractBoolClause_Filter() {
        Query query = Query.of(q -> q.bool(b -> b
            .filter(Query.of(fq -> fq.range(r -> r.field("price").gte(JsonData.of(10)))))
        ));
        
        List<Object> filter = OpenSearchQueryHelper.extractBoolClause(query, "bool", "filter");
        assertNotNull(filter);
        assertEquals(1, filter.size());
    }

    @Test
    void testExtractBoolClause_EmptyClause() {
        Query query = Query.of(q -> q.bool(b -> b));
        
        List<Object> must = OpenSearchQueryHelper.extractBoolClause(query, "bool", "must");
        assertNotNull(must);
        assertTrue(must.isEmpty());
    }

    @Test
    void testExtractTypedFieldValue_String() {
        Query query = Query.of(q -> q.term(t -> t.field("name").value(FieldValue.of("hello"))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals("hello", value);
        assertTrue(value instanceof String);
    }

    @Test
    void testExtractTypedFieldValue_Long() {
        Query query = Query.of(q -> q.term(t -> t.field("count").value(FieldValue.of(42L))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals(42L, value);
        assertTrue(value instanceof Long);
    }

    @Test
    void testExtractTypedFieldValue_Double() {
        Query query = Query.of(q -> q.term(t -> t.field("price").value(FieldValue.of(3.14))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals(3.14, value);
        assertTrue(value instanceof Double);
    }

    @Test
    void testExtractTypedFieldValue_Boolean() {
        Query query = Query.of(q -> q.term(t -> t.field("active").value(FieldValue.of(true))));
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals(true, value);
        assertTrue(value instanceof Boolean);
    }

    // ===== Integration tests with SearchRequest =====

    @Test
    void testExtractFromSearchRequest_Term() {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(q -> q.term(t -> t.field("category").value(FieldValue.of("electronics"))))
            .build();
        
        Query query = request.query();
        assertNotNull(query);
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Term", kind);
        
        String field = OpenSearchQueryHelper.extractFieldName(query, "term");
        assertEquals("category", field);
        
        Object value = OpenSearchQueryHelper.extractFieldValue(query, "term");
        assertEquals("electronics", value);
    }

    @Test
    void testExtractFromSearchRequest_Bool() {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(q -> q.bool(b -> b
                .must(Query.of(mq -> mq.term(t -> t.field("status").value(FieldValue.of("active")))))
                .filter(Query.of(fq -> fq.range(r -> r.field("price").gte(JsonData.of(10)).lte(JsonData.of(100)))))
            ))
            .build();
        
        Query query = request.query();
        assertNotNull(query);
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Bool", kind);
        
        List<Object> must = OpenSearchQueryHelper.extractBoolClause(query, "bool", "must");
        assertEquals(1, must.size());
        
        List<Object> filter = OpenSearchQueryHelper.extractBoolClause(query, "bool", "filter");
        assertEquals(1, filter.size());
    }

    @Test
    void testExtractFromSearchRequest_Exists() {
        SearchRequest request = new SearchRequest.Builder()
            .index("users")
            .query(q -> q.exists(e -> e.field("email")))
            .build();
        
        Query query = request.query();
        String kind = OpenSearchQueryHelper.extractQueryKind(query);
        assertEquals("Exists", kind);
        
        String field = OpenSearchQueryHelper.extractExistsField(query, "exists");
        assertEquals("email", field);
    }
}
