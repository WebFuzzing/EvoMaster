package org.evomaster.client.java.controller.opensearch;

import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.operations.TermOperation;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenSearchQueryParserTest {

    private final OpenSearchQueryParser parser = new OpenSearchQueryParser();

    @Test
    void parsesStringTermQuery() {
        Query query = Query.of(q -> q.term(t -> t
            .field("name")
            .value(FieldValue.of("Alice"))));

        QueryOperation operation = parser.parse(query);

        TermOperation<?> term = assertInstanceOf(TermOperation.class, operation);
        assertEquals("name", term.getFieldName());
        assertEquals("Alice", term.getValue());
    }

    @Test
    void parsesLongTermQuery() {
        Query query = Query.of(q -> q.term(t -> t
            .field("age")
            .value(FieldValue.of(42L))));

        QueryOperation operation = parser.parse(query);

        TermOperation<?> term = assertInstanceOf(TermOperation.class, operation);
        assertEquals("age", term.getFieldName());
        assertEquals(42L, term.getValue());
    }

    @Test
    void returnsNullForUnsupportedQueryKind() {
        Query query = Query.of(q -> q.match(m -> m
            .field("name")
            .query(FieldValue.of("Alice"))));

        assertNull(parser.parse(query));
    }
}
