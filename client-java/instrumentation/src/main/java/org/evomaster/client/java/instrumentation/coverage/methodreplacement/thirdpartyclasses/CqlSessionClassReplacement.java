package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.ExecutedCqlCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class CqlSessionClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final CqlSessionClassReplacement singleton = new CqlSessionClassReplacement();

    public static final String CASSANDRA_EXECUTE_STRING_SYNC = "cassandraExecuteStringSync";
    public static final String CASSANDRA_EXECUTE_STRING_POSITIONAL_VALUES_SYNC = "cassandraExecuteStringPositionalValuesSync";
    public static final String CASSANDRA_EXECUTE_STRING_NAMED_VALUES_SYNC = "cassandraExecuteStringNamedValuesSync";
    public static final String CASSANDRA_EXECUTE_STATEMENT_SYNC = "cassandraExecuteStatementSync";

    private static final String RESULT_SET_CLASS = "com.datastax.oss.driver.api.core.cql.ResultSet";
    private static final String STATEMENT_CLASS = "com.datastax.oss.driver.api.core.cql.Statement";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.datastax.oss.driver.api.core.CqlSession";
    }

    @Replacement(type = ReplacementType.TRACKER, id = CASSANDRA_EXECUTE_STRING_SYNC, usageFilter = UsageFilter.ANY, category = ReplacementCategory.CASSANDRA, castTo = RESULT_SET_CLASS)
    public static Object execute(Object cqlSession, String query) {
        return handleCqlExecute(CASSANDRA_EXECUTE_STRING_SYNC, cqlSession, query, query);
    }

    @Replacement(type = ReplacementType.TRACKER, id = CASSANDRA_EXECUTE_STRING_POSITIONAL_VALUES_SYNC, usageFilter = UsageFilter.ANY, category = ReplacementCategory.CASSANDRA, castTo = RESULT_SET_CLASS)
    public static Object execute(Object cqlSession, String query, Object... values) {
        return handleCqlExecute(CASSANDRA_EXECUTE_STRING_POSITIONAL_VALUES_SYNC, cqlSession, query, query, values);
    }

    @Replacement(type = ReplacementType.TRACKER, id = CASSANDRA_EXECUTE_STRING_NAMED_VALUES_SYNC, usageFilter = UsageFilter.ANY, category = ReplacementCategory.CASSANDRA, castTo = RESULT_SET_CLASS)
    public static Object execute(Object cqlSession, String query, Map<String, Object> values) {
        return handleCqlExecute(CASSANDRA_EXECUTE_STRING_NAMED_VALUES_SYNC, cqlSession, query, query, values);
    }

    @Replacement(type = ReplacementType.TRACKER, id = CASSANDRA_EXECUTE_STATEMENT_SYNC, usageFilter = UsageFilter.ANY, category = ReplacementCategory.CASSANDRA, castTo = RESULT_SET_CLASS)
    public static Object execute(Object cqlSession, @ThirdPartyCast(actualType = STATEMENT_CLASS) Object statement) {
        return handleCqlExecute(CASSANDRA_EXECUTE_STATEMENT_SYNC, cqlSession, extractQueryText(statement), statement);
    }

    private static Object handleCqlExecute(String id, Object cqlSession, String queryForTracking, Object... invokeArgs) {
        long start = System.currentTimeMillis();
        try {
            Method executeMethod = retrieveExecuteMethod(id, cqlSession);
            Object result = executeMethod.invoke(cqlSession, invokeArgs);
            long end = System.currentTimeMillis();
            long executionTime = end - start;
            ExecutedCqlCommand info = new ExecutedCqlCommand(queryForTracking, false, executionTime);
            ExecutionTracer.addCqlInfo(info);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    /**
     * Statement is a generic driver type; only SimpleStatement exposes the original
     * CQL text directly, while BoundStatement requires going through its PreparedStatement.
     */
    private static String extractQueryText(Object statement) {
        try {
            Method getQuery = statement.getClass().getMethod("getQuery");
            return (String) getQuery.invoke(statement);
        } catch (ReflectiveOperationException e) {
            // not a SimpleStatement (eg BoundStatement) -- fall through
        }
        try {
            Method getPreparedStatement = statement.getClass().getMethod("getPreparedStatement");
            Object prepared = getPreparedStatement.invoke(statement);
            Method getQuery = prepared.getClass().getMethod("getQuery");
            return (String) getQuery.invoke(prepared);
        } catch (ReflectiveOperationException e) {
            return statement.toString(); // eg BatchStatement -- best effort
        }
    }

    private static Method retrieveExecuteMethod(String id, Object cqlSession){
        return getOriginal(singleton, id, cqlSession);
    }

}