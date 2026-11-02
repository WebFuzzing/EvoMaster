package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Neo4j driver method replacements.
 * Provides common functionality for tracking Neo4j database operations.
 * <p>
 * In the driver, the {@code run} overloads are declared once and implemented by several types:
 * {@code Session} (auto-commit), {@code Transaction} (explicit transactions) and {@code TransactionContext}
 * (managed transactions), all reachable through the {@code QueryRunner} interface. The bytecode of a call
 * site names the static type of the receiver, and replacements are matched on that exact name, so each of
 * those types needs its own replacement class. They all share the tracking logic below; the subclasses only
 * declare the annotated overloads, since replaced methods are looked up per declaring class.
 */
public abstract class Neo4JOperationClassReplacement extends ThirdPartyMethodReplacementClass {

    protected static final String ID_RUN_STRING = "runString";
    protected static final String ID_RUN_STRING_MAP = "runStringMap";
    protected static final String ID_RUN_STRING_VALUE = "runStringValue";
    protected static final String ID_RUN_STRING_RECORD = "runStringRecord";
    protected static final String ID_RUN_QUERY = "runQuery";

    private static final String AS_MAP_METHOD = "asMap";
    private static final String AS_OBJECT_METHOD = "asObject";

    /**
     * Tracks a Neo4j database operation by recording the query, parameters, execution time, and success status.
     *
     * @param query the Cypher query string
     * @param parameters the query parameters, as the driver overload took them
     * @param successfullyExecuted whether the query executed successfully
     * @param executionTime the execution time in milliseconds
     */
    protected static void handleNeo4J(String query, Object parameters, boolean successfullyExecuted, long executionTime) {
        Neo4JRunCommand info = new Neo4JRunCommand(query, toPlainParameters(parameters), successfullyExecuted, executionTime);
        ExecutionTracer.addNeo4JInfo(info);
    }

    /**
     * Turns the parameters of a query into plain Java values by name, whatever shape the driver
     * overload took them in: a {@code Map} (whose values may be driver {@code Value}s), a
     * {@code Value} holding a map, a {@code Record}, or {@code null}. The driver's types cannot be
     * serialized, and the tracked command is when the SUT runs in a different process than the
     * controller. The driver is reached through reflection, so there is no dependency on it.
     *
     * @param parameters the parameters as passed to {@code run}
     * @return the parameters by name; empty when there are none, or when they are of a shape the
     *         driver never produces, as this runs inside the SUT and must not fail it
     */
    protected static Map<String, Object> toPlainParameters(Object parameters) {
        if (parameters == null) {
            return Collections.emptyMap();
        }
        try {
            Object map = parameters instanceof Map ? parameters : invoke(parameters, AS_MAP_METHOD);
            Map<String, Object> plain = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) map).entrySet()) {
                plain.put(String.valueOf(e.getKey()), toPlainValue(e.getValue()));
            }
            return plain;
        } catch (ReflectiveOperationException | RuntimeException e) {
            SimpleLogger.uniqueWarn("Failed to read the parameters of a Cypher query ("
                    + parameters.getClass().getName() + "): " + e.getMessage());
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

    /**
     * Invokes the original {@code run} method on the runner and tracks the query execution.
     *
     * @param singleton the replacement class instance, used to look up the original method
     * @param id the method identifier
     * @param runner the object the query is run on (a {@code Session}, {@code Transaction}, ...)
     * @param query the Cypher query string
     * @param parameters the query parameters
     * @param args the arguments of the original method
     * @return the Result object
     */
    protected static Object handleRun(ThirdPartyMethodReplacementClass singleton, String id, Object runner,
                                      String query, Object parameters, List<Object> args) {
        long start = System.currentTimeMillis();
        try {
            Method runMethod = getOriginal(singleton, id, runner);
            Object result = runMethod.invoke(runner, args.toArray());
            long end = System.currentTimeMillis();
            handleNeo4J(query, parameters, true, end - start);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * Extracts the Cypher query text from a Query object using reflection.
     *
     * @param queryObject the Query object
     * @return the query text
     * @throws RuntimeException if extraction fails
     */
    protected static String extractQueryText(Object queryObject) {
        try {
            Method textMethod = queryObject.getClass().getMethod("text");
            return (String) textMethod.invoke(queryObject);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to extract query text from Neo4j Query object", e);
        }
    }

    /**
     * Extracts the parameters from a Query object using reflection.
     *
     * @param queryObject the Query object
     * @return the parameters
     * @throws RuntimeException if extraction fails
     */
    protected static Object extractQueryParameters(Object queryObject) {
        try {
            Method parametersMethod = queryObject.getClass().getMethod("parameters");
            return parametersMethod.invoke(queryObject);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to extract parameters from Neo4j Query object", e);
        }
    }

}
