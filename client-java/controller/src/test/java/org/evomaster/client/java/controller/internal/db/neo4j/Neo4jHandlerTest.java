package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator;
import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link Neo4jHandler} and {@link Neo4jGraphReader} end-to-end against a hand-rolled fake
 * driver that exposes the same method names the reader reflects over ({@code session}/{@code run}/
 * {@code list}/{@code get}/{@code asString}/{@code asList}/{@code asMap}/{@code close}). This validates
 * the reflection plumbing and the whole parse, score and report pipeline without needing a live Neo4j
 * instance.
 * <p>
 * Integers are returned as {@code Long} to mimic the real driver's value mapping.
 */
class Neo4jHandlerTest {

    private static final String MATCH_QUERY =
            "MATCH (a:Person {age: 25})-[r:KNOWS]->(b:Person) WHERE b.age > 30 RETURN b";

    private FakeDriver example1Driver() {
        List<FakeRecord> nodes = Arrays.asList(
                nodeRecord("n1", labels("Person"), props("age", 25L, "name", "Ana")),
                nodeRecord("n2", labels("Person"), props("age", 28L, "name", "Luis")),
                nodeRecord("n3", labels("Animal"), props("age", 5L, "name", "Rex")),
                nodeRecord("n4", labels("Person"), props("age", 40L, "name", "Carlos")));
        List<FakeRecord> rels = Arrays.asList(
                relRecord("e1", "KNOWS", "n1", "n2"),
                relRecord("e2", "LIKES", "n1", "n3"),
                relRecord("e3", "KNOWS", "n3", "n4"));
        return new FakeDriver(nodes, rels);
    }

    @Test
    void testScoresMatchQueryAgainstLiveGraph() {
        Neo4jHandler handler = new Neo4jHandler();
        handler.setNeo4jConnection(example1Driver());
        handler.handle(new Neo4JRunCommand(MATCH_QUERY, null, true, 1));

        List<Neo4jCommandWithDistance> evaluated = handler.getEvaluatedNeo4jCommands();

        assertEquals(1, evaluated.size());
        Neo4jCommandWithDistance result = evaluated.get(0);
        assertEquals(MATCH_QUERY, result.getCommand());
        assertFalse(result.getDistanceWithMetrics().isEvaluationFailure());
        assertEquals(4, result.getDistanceWithMetrics().getNumberOfEvaluatedNodes());
        // distance is 1 - ofTrue, and here ofTrue is about 0.939.
        assertEquals(0.061, result.getDistanceWithMetrics().getDistance(), 0.005);
    }

    @Test
    void testNonMatchQueryIsSkipped() {
        Neo4jHandler handler = new Neo4jHandler();
        handler.setNeo4jConnection(example1Driver());
        handler.handle(new Neo4JRunCommand("CREATE (n:Person {name: 'Zoe'})", null, true, 1));
        handler.handle(new Neo4JRunCommand(MATCH_QUERY, null, true, 1));

        List<Neo4jCommandWithDistance> evaluated = handler.getEvaluatedNeo4jCommands();
        // The write query does not parse as a MATCH and is skipped; only the read query is scored.
        assertEquals(1, evaluated.size());
        assertEquals(MATCH_QUERY, evaluated.get(0).getCommand());
    }

    @Test
    void testNoConnectionYieldsNoHeuristics() {
        Neo4jHandler handler = new Neo4jHandler();
        handler.handle(new Neo4JRunCommand(MATCH_QUERY, null, true, 1));
        assertTrue(handler.getEvaluatedNeo4jCommands().isEmpty());
    }

    @Test
    void testHeuristicsAreNotComputedWhenDisabled() {
        Neo4jHandler handler = new Neo4jHandler();
        handler.setNeo4jConnection(example1Driver());
        handler.handle(new Neo4JRunCommand(MATCH_QUERY, null, true, 1));
        handler.setCalculateHeuristics(false);

        // Registered while enabled, so it is the check before evaluating that has to drop the query.
        assertTrue(handler.getEvaluatedNeo4jCommands().isEmpty());
    }

    @Test
    void testAFailedEvaluationReportsTheMaximumDistance() {
        // A relationship whose source node is not in the graph: the graph reads fine, but scoring the query
        // against it fails, and that failure must never look closer to satisfied than a genuine miss.
        List<FakeRecord> nodes = Arrays.asList(
                nodeRecord("n1", labels("Person"), props("age", 25L)));
        List<FakeRecord> rels = Arrays.asList(
                relRecord("e1", "KNOWS", "ghost", "n1"));
        Neo4jHandler handler = new Neo4jHandler();
        handler.setNeo4jConnection(new FakeDriver(nodes, rels));
        handler.handle(new Neo4JRunCommand(MATCH_QUERY, null, true, 1));

        List<Neo4jCommandWithDistance> evaluated = handler.getEvaluatedNeo4jCommands();

        assertEquals(1, evaluated.size());
        Neo4jDistanceWithMetrics metrics = evaluated.get(0).getDistanceWithMetrics();
        assertTrue(metrics.isEvaluationFailure());
        assertEquals(Neo4jHeuristicsCalculator.MAX_NEO4J_DISTANCE, metrics.getDistance(), 0.0d);
        assertEquals(1, metrics.getNumberOfEvaluatedNodes());
    }

    @Test
    void testAnUnreadableGraphYieldsNoHeuristicsInsteadOfFailing() {
        Neo4jHandler handler = new Neo4jHandler();
        handler.setNeo4jConnection(new BrokenDriver());
        handler.handle(new Neo4JRunCommand(MATCH_QUERY, null, true, 1));

        // The SUT must keep running even if its driver cannot be queried.
        assertTrue(handler.getEvaluatedNeo4jCommands().isEmpty());
    }

    // Fake Neo4j driver, exposing only the methods the reader reflects over.

    public static final class FakeDriver {
        private final List<FakeRecord> nodes;
        private final List<FakeRecord> rels;

        FakeDriver(List<FakeRecord> nodes, List<FakeRecord> rels) {
            this.nodes = nodes;
            this.rels = rels;
        }

        public FakeSession session() {
            return new FakeSession(nodes, rels);
        }
    }

    /** A driver whose session cannot be opened, standing in for a Neo4j that is down. */
    public static final class BrokenDriver {

        public Object session() {
            throw new IllegalStateException("no connection to the database");
        }
    }

    public static final class FakeSession {
        private final List<FakeRecord> nodes;
        private final List<FakeRecord> rels;

        FakeSession(List<FakeRecord> nodes, List<FakeRecord> rels) {
            this.nodes = nodes;
            this.rels = rels;
        }

        public FakeResult run(String query) {
            return new FakeResult(query.contains("labels(n)") ? nodes : rels);
        }

        public void close() {
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

    private static FakeRecord relRecord(String id, String type, String src, String tgt) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("type", type);
        f.put("src", src);
        f.put("tgt", tgt);
        f.put("props", new LinkedHashMap<String, Object>());
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
