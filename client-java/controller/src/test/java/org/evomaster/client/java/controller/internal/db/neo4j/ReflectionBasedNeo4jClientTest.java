package org.evomaster.client.java.controller.internal.db.neo4j;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that {@link ReflectionBasedNeo4jClient} finds and calls the driver's methods by reflection.
 * The fakes below stand in for {@code Driver} / {@code Session} / {@code Result} / {@code Record} /
 * {@code Value}: they are matched purely by method name and signature, which is exactly what the
 * client does against the real driver.
 */
class ReflectionBasedNeo4jClientTest {

    @Test
    void testOpensAndClosesASession() {
        FakeDriver driver = new FakeDriver();
        ReflectionBasedNeo4jClient client = new ReflectionBasedNeo4jClient(driver);

        Object session = client.session();
        assertSame(driver.lastSession, session);
        assertFalse(driver.lastSession.closed);

        client.close(session);
        assertTrue(driver.lastSession.closed);
    }

    @Test
    void testRunsAQueryAndReturnsItsRecords() {
        FakeDriver driver = new FakeDriver(record("id", "n1"));
        ReflectionBasedNeo4jClient client = new ReflectionBasedNeo4jClient(driver);

        Object session = client.session();
        List<?> records = client.runAndList(session, "MATCH (n) RETURN n");

        assertEquals(1, records.size());
        assertEquals("MATCH (n) RETURN n", driver.lastSession.lastQuery);
        assertNull(driver.lastSession.lastParameters);
    }

    @Test
    void testRunsAParameterisedQueryAndPassesTheValuesAlong() {
        // The values must travel as a parameter map, not interpolated into the query text.
        FakeDriver driver = new FakeDriver();
        ReflectionBasedNeo4jClient client = new ReflectionBasedNeo4jClient(driver);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("title", "The Matrix");

        Object session = client.session();
        client.runAndList(session, "MATCH (m:Movie {title: $title}) RETURN m", params);

        assertEquals("MATCH (m:Movie {title: $title}) RETURN m", driver.lastSession.lastQuery);
        assertEquals(params, driver.lastSession.lastParameters);
    }

    @Test
    void testDetachDeleteAllEmptiesTheDatabaseAndClosesItsSession() {
        FakeDriver driver = new FakeDriver();
        ReflectionBasedNeo4jClient client = new ReflectionBasedNeo4jClient(driver);

        client.detachDeleteAll();

        assertEquals("MATCH (n) DETACH DELETE n", driver.lastSession.lastQuery);
        assertTrue(driver.lastSession.closed);
    }

    @Test
    void testReadsTheFieldsOfARecord() {
        FakeDriver driver = new FakeDriver();
        ReflectionBasedNeo4jClient client = new ReflectionBasedNeo4jClient(driver);
        FakeRecord record = record("name", "Ana", "labels", Arrays.asList("Person"), "props", props("age", 25));

        assertEquals("Ana", client.asString(client.get(record, "name")));
        assertEquals(Arrays.asList("Person"), client.asList(client.get(record, "labels")));
        assertEquals(props("age", 25), client.asMap(client.get(record, "props")));
    }

    @Test
    void testAMissingDriverMethodFailsWithAClearMessage() {
        // A driver whose API does not match: the failure has to name the method, not surface a bare
        // NoSuchMethodException from somewhere inside the reflection.
        ReflectionBasedNeo4jClient client = new ReflectionBasedNeo4jClient(new Object());

        RuntimeException e = assertThrows(RuntimeException.class, client::session);
        assertTrue(e.getMessage().contains("session"), e.getMessage());
    }

    @Test
    void testANullDriverIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ReflectionBasedNeo4jClient(null));
    }

    public static final class FakeDriver {
        private final List<FakeRecord> records;
        FakeSession lastSession;

        FakeDriver(FakeRecord... records) {
            this.records = new ArrayList<>(Arrays.asList(records));
        }

        public FakeSession session() {
            lastSession = new FakeSession(records);
            return lastSession;
        }
    }

    public static final class FakeSession {
        private final List<FakeRecord> records;
        String lastQuery;
        Map<String, Object> lastParameters;
        boolean closed = false;

        FakeSession(List<FakeRecord> records) {
            this.records = records;
        }

        public FakeResult run(String query) {
            lastQuery = query;
            lastParameters = null;
            return new FakeResult(records);
        }

        public FakeResult run(String query, Map<String, Object> parameters) {
            lastQuery = query;
            lastParameters = parameters;
            return new FakeResult(records);
        }

        public void close() {
            closed = true;
        }
    }

    public static final class FakeResult {
        private final List<FakeRecord> records;

        FakeResult(List<FakeRecord> records) {
            this.records = records;
        }

        public List<FakeRecord> list() {
            return records;
        }
    }

    public static final class FakeRecord {
        private final Map<String, Object> fields;

        FakeRecord(Map<String, Object> fields) {
            this.fields = fields;
        }

        public FakeValue get(String key) {
            return new FakeValue(fields.get(key));
        }
    }

    public static final class FakeValue {
        private final Object value;

        FakeValue(Object value) {
            this.value = value;
        }

        public String asString() {
            return (String) value;
        }

        @SuppressWarnings("unchecked")
        public List<Object> asList() {
            return (List<Object>) value;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> asMap() {
            return (Map<String, Object>) value;
        }
    }

    private static FakeRecord record(Object... kv) {
        return new FakeRecord(props(kv));
    }

    private static Map<String, Object> props(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
