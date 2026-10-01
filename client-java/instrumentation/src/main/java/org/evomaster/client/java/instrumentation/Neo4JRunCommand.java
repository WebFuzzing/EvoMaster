package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.utils.SimpleLogger;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Info related to Neo4J RUN command execution.
 */
public class Neo4JRunCommand implements Serializable {

    private static final String AS_MAP_METHOD = "asMap";
    private static final String AS_OBJECT_METHOD = "asObject";

    /**
     * Executed RUN query (Cypher query string)
     */
    private final String query;

    /**
     * Query parameters by name, as plain Java values. The driver's own types are never kept, since
     * this object is serialized when the SUT runs in a different process than the controller.
     */
    private final Map<String, Object> parameters;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    /**
     * @param parameters the parameters as the driver overload took them: a {@code Map}, a
     *                   {@code Value} holding a map, a {@code Record}, or {@code null}
     */
    public Neo4JRunCommand(String query, Object parameters, boolean successfullyExecuted, long executionTime) {
        this.query = query;
        this.parameters = toPlainMap(parameters);
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public boolean getSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    /**
     * Reads the parameters through reflection, so there is no dependency on the driver. A shape the
     * driver never produces yields no parameters, as this runs inside the SUT and must not fail it.
     */
    private static Map<String, Object> toPlainMap(Object captured) {
        if (captured == null) {
            return Collections.emptyMap();
        }
        try {
            Object map = captured instanceof Map ? captured : invoke(captured, AS_MAP_METHOD);
            Map<String, Object> plain = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) map).entrySet()) {
                plain.put(String.valueOf(e.getKey()), toPlainValue(e.getValue()));
            }
            return plain;
        } catch (ReflectiveOperationException | RuntimeException e) {
            SimpleLogger.uniqueWarn("Failed to read the parameters of a Cypher query ("
                    + captured.getClass().getName() + "): " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * A driver {@code Value} becomes the Java object it wraps, maps and lists are copied with their
     * content, and what still cannot be serialized is kept as text.
     */
    private static Object toPlainValue(Object value) throws ReflectiveOperationException {
        if (value == null) {
            return null;
        }
        if (hasMethod(value, AS_OBJECT_METHOD)) {
            return toPlainValue(invoke(value, AS_OBJECT_METHOD));
        }
        if (value instanceof Map) {
            Map<String, Object> plain = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                plain.put(String.valueOf(e.getKey()), toPlainValue(e.getValue()));
            }
            return plain;
        }
        if (value instanceof Iterable) {
            List<Object> plain = new ArrayList<>();
            for (Object element : (Iterable<?>) value) {
                plain.add(toPlainValue(element));
            }
            return plain;
        }
        return value instanceof Serializable ? value : value.toString();
    }

    private static boolean hasMethod(Object target, String method) {
        try {
            target.getClass().getMethod(method);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        Method m = target.getClass().getMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }
}
