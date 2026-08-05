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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CqlSessionClassReplacement extends ThirdPartyMethodReplacementClass {
    private static final CqlSessionClassReplacement singleton = new CqlSessionClassReplacement();

    public static final String CASSANDRA_EXECUTE_STRING_SYNC = "cassandraExecuteStringSync";
    public static final String CASSANDRA_EXECUTE_STRING_POSITIONAL_VALUES_SYNC = "cassandraExecuteStringPositionalValuesSync";
    public static final String CASSANDRA_EXECUTE_STRING_NAMED_VALUES_SYNC = "cassandraExecuteStringNamedValuesSync";
    public static final String CASSANDRA_EXECUTE_STATEMENT_SYNC = "cassandraExecuteStatementSync";

    private static final String RESULT_SET_CLASS = "com.datastax.oss.driver.api.core.cql.ResultSet";
    private static final String STATEMENT_CLASS = "com.datastax.oss.driver.api.core.cql.Statement";

    // a CQL identifier: either quoted+case-sensitive (e.g. "Tbl") or unquoted (e.g. tbl)
    private static final String IDENTIFIER = "(?:\"[^\"]+\"|[A-Za-z_]\\w*)";
    // clause keywords that introduce a table reference (SELECT/DELETE ... FROM, INSERT ... INTO, UPDATE ...)
    private static final String CLAUSE_KEYWORDS = "(?:FROM|INTO|UPDATE)";

    /**
     * Matches the keyspace/table reference after FROM (SELECT/DELETE), INTO (INSERT), or
     * UPDATE, e.g. "FROM ks.tbl", "INTO tbl", "UPDATE \"Ks\".\"Tbl\"". Group "first" is the
     * keyspace when a "keyspace.table" qualifier ("second") is present, otherwise it's the
     * table name on its own.
     */
    private static final Pattern TABLE_REFERENCE_PATTERN = Pattern.compile(
            "(?i)\\b" + CLAUSE_KEYWORDS + "\\s+"
                    + "(?<first>" + IDENTIFIER + ")"
                    + "(?:\\s*\\.\\s*(?<second>" + IDENTIFIER + "))?"
    );

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
            TableReference ref = extractTableReference(queryForTracking);
            ExecutedCqlCommand info = new ExecutedCqlCommand(queryForTracking, ref.keyspaceName, ref.tableName, false, executionTime);
            ExecutionTracer.addCqlInfo(info);
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    /**
     * Best-effort extraction of keyspace/table from the CQL text. Returns both fields as
     * null when the query doesn't match a recognised SELECT/INSERT/UPDATE/DELETE shape
     * (eg DDL, batches).
     */
    private static TableReference extractTableReference(String query) {
        if (query == null) {
            return new TableReference(null, null);
        }

        Matcher matcher = TABLE_REFERENCE_PATTERN.matcher(query);
        if (!matcher.find()) {
            return new TableReference(null, null);
        } else {
            String first = stripQuotes(matcher.group("first"));
            String second = matcher.group("second") != null ? stripQuotes(matcher.group("second")) : null;
            // if there is an "a.b" qualifier, "a" is the keyspace and "b" is the table;
            // otherwise the single identifier is the table, and the keyspace is the session default
            return second != null ? new TableReference(first, second) : new TableReference(null, first);
        }
    }

    private static String stripQuotes(String identifier) {
        if (identifier.length() >= 2 && identifier.charAt(0) == '"' && identifier.charAt(identifier.length() - 1) == '"') {
            return identifier.substring(1, identifier.length() - 1);
        } else {
            return identifier;
        }
    }

    private static class TableReference {
        final String keyspaceName;
        final String tableName;

        TableReference(String keyspaceName, String tableName) {
            this.keyspaceName = keyspaceName;
            this.tableName = tableName;
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