package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.Neo4JRunCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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

    /**
     * Tracks a Neo4j database operation by recording the query, parameters, execution time, and success status.
     *
     * @param query the Cypher query string
     * @param parameters the query parameters
     * @param successfullyExecuted whether the query executed successfully
     * @param executionTime the execution time in milliseconds
     */
    protected static void handleNeo4J(String query, Object parameters, boolean successfullyExecuted, long executionTime) {
        Neo4JRunCommand info = new Neo4JRunCommand(query, parameters, successfullyExecuted, executionTime);
        ExecutionTracer.addNeo4JInfo(info);
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
