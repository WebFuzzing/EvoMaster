package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link Neo4jGraphReader} against a hand-rolled fake driver that exposes the same method
 * names the reader reflects over ({@code session}/{@code run}/{@code list}/{@code get}/{@code asString}/
 * {@code asList}/{@code asMap}/{@code close}). This validates the reflection plumbing in isolation, with
 * no parsing/scoring involved, and without needing a live Neo4j instance.
 * <p>
 * Integers are returned as {@code Long} to mimic the real driver's value mapping.
 */
class Neo4jGraphReaderTest {

    @Test
    void testReadsNodesWithLabelsAndProperties() {
        List<FakeRecord> nodes = Arrays.asList(
                nodeRecord("n1", labels("Person"), props("age", 25L, "name", "Ana")),
                nodeRecord("n2", labels("Person", "Employee"), props("age", 40L, "name", "Carlos")));
        FakeDriver driver = new FakeDriver(nodes, Collections.emptyList());

        Neo4jGraph graph = new Neo4jGraphReader().read(driver);

        assertEquals(2, graph.nodeCount());
        Neo4jNode n1 = graph.getNodeById("n1");
        assertNotNull(n1);
        assertTrue(n1.getLabels().contains("Person"));
        assertEquals(25L, n1.getProperty("age"));
        assertEquals("Ana", n1.getProperty("name"));

        Neo4jNode n2 = graph.getNodeById("n2");
        assertNotNull(n2);
        assertTrue(n2.getLabels().containsAll(Arrays.asList("Person", "Employee")));
    }

    @Test
    void testReadsEdgesWithTypeAndEndpoints() {
        List<FakeRecord> nodes = Arrays.asList(
                nodeRecord("n1", labels("Person"), props()),
                nodeRecord("n2", labels("Person"), props()));
        List<FakeRecord> rels = Arrays.asList(
                relRecord("e1", "KNOWS", "n1", "n2", props("since", 2020L)));
        FakeDriver driver = new FakeDriver(nodes, rels);

        Neo4jGraph graph = new Neo4jGraphReader().read(driver);

        assertEquals(1, graph.getEdges().size());
        Neo4jEdge e1 = graph.getEdges().get(0);
        assertEquals("e1", e1.getId());
        assertEquals("KNOWS", e1.getType());
        assertEquals("n1", e1.getSourceId());
        assertEquals("n2", e1.getTargetId());
        assertEquals(2020L, e1.getProperty("since"));
    }

    @Test
    void testEmptyGraphReadsAsEmpty() {
        FakeDriver driver = new FakeDriver(Collections.emptyList(), Collections.emptyList());

        Neo4jGraph graph = new Neo4jGraphReader().read(driver);

        assertEquals(0, graph.nodeCount());
        assertTrue(graph.getEdges().isEmpty());
    }

    @Test
    void testSessionIsClosedAfterReading() {
        FakeDriver driver = new FakeDriver(Collections.emptyList(), Collections.emptyList());

        new Neo4jGraphReader().read(driver);

        assertTrue(driver.lastSession.closed);
    }

    // --- fake Neo4j driver (only the methods the reader reflects over) ---------------------------

    public static final class FakeDriver {
        private final List<FakeRecord> nodes;
        private final List<FakeRecord> rels;
        FakeSession lastSession;

        FakeDriver(List<FakeRecord> nodes, List<FakeRecord> rels) {
            this.nodes = nodes;
            this.rels = rels;
        }

        public FakeSession session() {
            lastSession = new FakeSession(nodes, rels);
            return lastSession;
        }
    }

    public static final class FakeSession {
        private final List<FakeRecord> nodes;
        private final List<FakeRecord> rels;
        boolean closed = false;

        FakeSession(List<FakeRecord> nodes, List<FakeRecord> rels) {
            this.nodes = nodes;
            this.rels = rels;
        }

        public FakeResult run(String query) {
            return new FakeResult(query.contains("labels(n)") ? nodes : rels);
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

    private static FakeRecord nodeRecord(String id, List<Object> labels, Map<String, Object> props) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("labels", labels);
        f.put("props", props);
        return new FakeRecord(f);
    }

    private static FakeRecord relRecord(String id, String type, String src, String tgt, Map<String, Object> props) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("type", type);
        f.put("src", src);
        f.put("tgt", tgt);
        f.put("props", props);
        return new FakeRecord(f);
    }

    private static List<Object> labels(String... ls) {
        return new ArrayList<>(Arrays.asList(ls));
    }

    private static Map<String, Object> props(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
