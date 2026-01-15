package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class Neo4JClassReplacement extends Neo4JOperationClassReplacement {
    private static final Neo4JClassReplacement singleton = new Neo4JClassReplacement();

    private static final String ID_RUN_STRING = "runString";
    private static final String ID_RUN_STRING_MAP = "runStringMap";
    private static final String ID_RUN_STRING_VALUE = "runStringValue";
    private static final String ID_RUN_STRING_RECORD = "runStringRecord";
    private static final String ID_RUN_QUERY = "runQuery";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.neo4j.driver.Session";
    }

    // Result run(String query)
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object session, String query) {
        return handleRun(ID_RUN_STRING, session, query, null, Collections.singletonList(query));
    }

    // Result run(String query, Map<String, Object> parameters)
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_MAP, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object session, String query, Map<String, Object> parameters) {
        return handleRun(ID_RUN_STRING_MAP, session, query, parameters, Arrays.asList(query, parameters));
    }

    // Result run(String query, Value parameters)
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_VALUE, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run_EM_0(Object session, String query, @ThirdPartyCast(actualType = "org.neo4j.driver.Value") Object parameters) {
        return handleRun(ID_RUN_STRING_VALUE, session, query, parameters, Arrays.asList(query, parameters));
    }

    // Result run(String query, Record parameters)
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_STRING_RECORD, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run_EM_1(Object session, String query, @ThirdPartyCast(actualType = "org.neo4j.driver.Record") Object parameters) {
        return handleRun(ID_RUN_STRING_RECORD, session, query, parameters, Arrays.asList(query, parameters));
    }

    // Result run(Query query)
    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = ID_RUN_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.NEO4J, castTo = "org.neo4j.driver.Result")
    public static Object run(Object session, @ThirdPartyCast(actualType = "org.neo4j.driver.Query") Object query) {
        String queryText = extractQueryText(query);
        Object parameters = extractQueryParameters(query);
        return handleRun(ID_RUN_QUERY, session, queryText, parameters, Collections.singletonList(query));
    }

    /**
     * Extract the Cypher query text from a Query object using reflection.
     */
    private static String extractQueryText(Object queryObject) {
        try {
            Method textMethod = queryObject.getClass().getMethod("text");
            return (String) textMethod.invoke(queryObject);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Fallback to toString if text() method is not available
            return queryObject.toString();
        }
    }

    /**
     * Extract the parameters from a Query object using reflection.
     */
    private static Object extractQueryParameters(Object queryObject) {
        try {
            Method parametersMethod = queryObject.getClass().getMethod("parameters");
            return parametersMethod.invoke(queryObject);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Return null if parameters() method is not available
            return null;
        }
    }

    private static Object handleRun(String id, Object session, String query, Object parameters, List<Object> args) {
        long start = System.currentTimeMillis();
        try {
            Method runMethod = retrieveRunMethod(id, session);
            Object result = runMethod.invoke(session, args.toArray());
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

    private static Method retrieveRunMethod(String id, Object session) {
        return getOriginal(singleton, id, session);
    }
}
