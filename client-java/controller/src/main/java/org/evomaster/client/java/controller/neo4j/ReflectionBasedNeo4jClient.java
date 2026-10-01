package org.evomaster.client.java.controller.neo4j;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    private static final String AS_LIST_METHOD = "asList";
    private static final String AS_OBJECT_METHOD = "asObject";
    private static final String AS_MAP_METHOD = "asMap";
    private static final String AS_STRING_METHOD = "asString";
    private static final String CLOSE_METHOD = "close";
    private static final String GET_METHOD = "get";
    private static final String LIST_METHOD = "list";
    private static final String RUN_METHOD = "run";
    private static final String SESSION_METHOD = "session";

    private static final String DETACH_DELETE_ALL_QUERY = "MATCH (n) DETACH DELETE n";

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
        return invoke(driver, SESSION_METHOD);
    }

    public void close(Object session) {
        invoke(session, CLOSE_METHOD);
    }

    /** Runs a read-only Cypher query and returns its records. */
    public List<?> runAndList(Object session, String cypher) {
        Object result = invoke(session, RUN_METHOD, new Class<?>[]{String.class}, cypher);
        return (List<?>) invoke(result, LIST_METHOD);
    }

    /**
     * Runs a parameterised Cypher query and returns its records. Values travel as a parameter map
     * rather than being interpolated into the query text, which avoids quoting and typing problems.
     *
     * @param parameters keys are parameter names without the leading {@code $}, values are the
     *                   values to bind to them
     */
    public List<?> runAndList(Object session, String cypher, Map<String, Object> parameters) {
        Object result = invoke(session, RUN_METHOD,
                new Class<?>[]{String.class, Map.class}, cypher, parameters);
        return (List<?>) invoke(result, LIST_METHOD);
    }

    /** Empties the database: every node and every relationship attached to it. */
    public void detachDeleteAll() {
        Object session = session();
        try {
            runAndList(session, DETACH_DELETE_ALL_QUERY);
        } finally {
            close(session);
        }
    }

    /** Reads the field {@code key} out of a {@code Record}. */
    public Object get(Object record, String key) {
        return invoke(record, GET_METHOD, new Class<?>[]{String.class}, key);
    }

    public String asString(Object value) {
        return (String) invoke(value, AS_STRING_METHOD);
    }

    public List<?> asList(Object value) {
        return (List<?>) invoke(value, AS_LIST_METHOD);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) invoke(value, AS_MAP_METHOD);
    }

    /**
     * Turns the parameters captured with a query into a plain map of Java values, whatever shape the
     * driver overload took them in: a {@code Map<String, Object>} (whose values may be driver
     * {@code Value}s), a {@code Value} holding a map, or a {@code Record}. Nested driver values are
     * unwrapped, so the result never references the driver's types.
     *
     * @param captured what {@code Neo4JRunCommand.getParameters()} holds, possibly {@code null}
     * @return the parameters by name; empty when there are none
     * @throws IllegalArgumentException if the captured object is of a shape the driver never produces
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parametersAsMap(Object captured) {
        if (captured == null) {
            return Collections.emptyMap();
        }
        if (captured instanceof Map) {
            Map<String, Object> plain = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) captured).entrySet()) {
                plain.put(e.getKey(), unwrap(e.getValue()));
            }
            return plain;
        }
        if (hasMethod(captured, AS_MAP_METHOD)) {
            return (Map<String, Object>) invokeStatic(captured, AS_MAP_METHOD);
        }
        throw new IllegalArgumentException("Unsupported Neo4j parameters type: " + captured.getClass().getName());
    }

    /** A driver {@code Value} becomes the Java object it wraps; anything else is already plain. */
    private static Object unwrap(Object value) {
        if (value != null && hasMethod(value, AS_OBJECT_METHOD)) {
            return invokeStatic(value, AS_OBJECT_METHOD);
        }
        return value;
    }

    private static boolean hasMethod(Object target, String method) {
        try {
            target.getClass().getMethod(method);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Object invokeStatic(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to call the Neo4j driver via reflection (" + method + ")", e);
        }
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
