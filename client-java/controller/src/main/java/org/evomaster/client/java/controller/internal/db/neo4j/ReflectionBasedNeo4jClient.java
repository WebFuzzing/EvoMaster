package org.evomaster.client.java.controller.internal.db.neo4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Talks to the SUT's Neo4j driver through reflection, so that EvoMaster never has a compile-time
 * dependency on {@code neo4j-java-driver} and cannot clash with whichever version the SUT ships.
 * Mirrors the approach of {@code ReflectionBasedRedisClient}: every {@code Class.forName} /
 * {@code getMethod} / {@code invoke} lives here, behind an ordinary typed API, so its callers hold
 * no reflection of their own.
 * <p>
 * Only primitive projections cross this boundary (ids, label and type names, property maps), never
 * the driver's own {@code Node} / {@code Relationship} types, which keeps the reflected surface down
 * to {@code Session.run}, {@code Result.list}, {@code Record.get} and {@code Value.as*}.
 */
public class ReflectionBasedNeo4jClient {

    /** The SUT's {@code org.neo4j.driver.Driver}, held as an {@code Object} on purpose. */
    private final Object driver;

    /**
     * @param driver the SUT's {@code org.neo4j.driver.Driver}
     */
    public ReflectionBasedNeo4jClient(Object driver) {
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        this.driver = driver;
    }

    /** Opens a session. The caller is responsible for passing it to {@link #close(Object)}. */
    public Object session() {
        return invoke(driver, "session");
    }

    public void close(Object session) {
        invoke(session, "close");
    }

    /** Runs a read-only Cypher query and returns its records. */
    public List<?> runAndList(Object session, String cypher) {
        Object result = invoke(session, "run", new Class<?>[]{String.class}, cypher);
        return (List<?>) invoke(result, "list");
    }

    /**
     * Runs a parameterised Cypher query and returns its records. Values travel as a parameter map
     * rather than being interpolated into the query text, which avoids quoting and typing problems.
     *
     * @param parameters keys are parameter names without the leading {@code $}, values are the
     *                   values to bind to them
     */
    public List<?> runAndList(Object session, String cypher, Map<String, Object> parameters) {
        Object result = invoke(session, "run",
                new Class<?>[]{String.class, Map.class}, cypher, parameters);
        return (List<?>) invoke(result, "list");
    }

    /** Empties the database: every node and every relationship attached to it. */
    public void detachDeleteAll() {
        Object session = session();
        try {
            runAndList(session, "MATCH (n) DETACH DELETE n");
        } finally {
            close(session);
        }
    }

    /** Reads the field {@code key} out of a {@code Record}. */
    public Object get(Object record, String key) {
        return invoke(record, "get", new Class<?>[]{String.class}, key);
    }

    public String asString(Object value) {
        return (String) invoke(value, "asString");
    }

    public List<?> asList(Object value) {
        return (List<?>) invoke(value, "asList");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) invoke(value, "asMap");
    }

    private Object invoke(Object target, String method) {
        return invoke(target, method, new Class<?>[0]);
    }

    private Object invoke(Object target, String method, Class<?>[] argTypes, Object... args) {
        try {
            Method m = target.getClass().getMethod(method, argTypes);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to call the Neo4j driver via reflection (" + method + ")", e);
        }
    }
}
