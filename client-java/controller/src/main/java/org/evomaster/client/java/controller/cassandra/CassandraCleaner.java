package org.evomaster.client.java.controller.cassandra;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes the data of Cassandra keyspaces, so that the state of
 * the database can be reset between tests.
 */
public class CassandraCleaner {

    private static final String CQL_IDENTIFIER_CLASS = "com.datastax.oss.driver.api.core.CqlIdentifier";

    /*
        Names of the driver methods invoked via reflection throughout this class.
     */
    private static final String METHOD_EXECUTE = "execute";
    private static final String METHOD_FROM_CQL = "fromCql";
    private static final String METHOD_GET = "get";
    private static final String METHOD_GET_KEYSPACE = "getKeyspace";
    private static final String METHOD_GET_METADATA = "getMetadata";
    private static final String METHOD_GET_NAME = "getName";
    private static final String METHOD_GET_TABLES = "getTables";
    private static final String METHOD_IS_PRESENT = "isPresent";
    private static final String METHOD_AS_CQL = "asCql";

    /**
     * The keyspaces Cassandra itself owns. Clearing one would break the cluster, so asking for it is
     * treated as a caller mistake rather than silently skipped.
     */
    private static final Set<String> SYSTEM_KEYSPACES = new HashSet<>(Arrays.asList(
            "system",
            "system_schema",
            "system_auth",
            "system_distributed",
            "system_traces",
            "system_views",
            "system_virtual_schema"
    ));

    private CassandraCleaner() {
    }

    /**
     * Truncates every table of each of the given keyspaces, leaving their schema untouched.
     *
     * @param cqlSession    a live {@code com.datastax.oss.driver.api.core.CqlSession}
     * @param keyspaceNames the keyspaces to clear, as written in CQL
     * @throws IllegalArgumentException if the session is null, if no keyspace is given, or if one of
     *                                  them is a system keyspace or does not exist
     */
    public static void clearKeyspaces(Object cqlSession, List<String> keyspaceNames) {

        if (cqlSession == null) {
            throw new IllegalArgumentException("No Cassandra session to clear the keyspaces with");
        }
        if (keyspaceNames == null || keyspaceNames.isEmpty()) {
            throw new IllegalArgumentException("No keyspace to clear");
        }
        keyspaceNames.forEach(CassandraCleaner::checkIsNotSystemKeyspace);

        /*
            The TRUNCATEs are not SUT traffic, so they are not to be traced. Note that a TRUNCATE is
            not buffered for heuristics anyway, as only SELECT, UPDATE and DELETE are, but the flag
            also keeps the schema tracer from treating a clean-up as a query worth capturing a table
            for.
         */
        ExecutionTracer.setExecutingInitCassandra(true);
        try {
            for (String keyspaceName : keyspaceNames) {
                truncateTablesOf(cqlSession, keyspaceName);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to clear the Cassandra keyspaces " + keyspaceNames, e);
        } finally {
            ExecutionTracer.setExecutingInitCassandra(false);
        }
    }

    private static void checkIsNotSystemKeyspace(String keyspaceName) {
        if (SYSTEM_KEYSPACES.contains(keyspaceName.toLowerCase())) {
            throw new IllegalArgumentException("Cannot clear the system keyspace " + keyspaceName);
        }
    }

    private static void truncateTablesOf(Object cqlSession, String keyspaceName) throws ReflectiveOperationException {

        Object keyspaceMetadata = resolveKeyspaceMetadata(cqlSession, keyspaceName);
        if (keyspaceMetadata == null) {
            throw new IllegalArgumentException("The keyspace " + keyspaceName + " does not exist");
        }

        String keyspace = asCql(invoke(keyspaceMetadata, METHOD_GET_NAME));

        Map<?, ?> tablesByName = (Map<?, ?>) invoke(keyspaceMetadata, METHOD_GET_TABLES);
        for (Object tableMetadata : tablesByName.values()) {
            String table = asCql(invoke(tableMetadata, METHOD_GET_NAME));
            execute(cqlSession, "TRUNCATE " + keyspace + "." + table);
        }
    }

    private static Object resolveKeyspaceMetadata(Object cqlSession, String keyspaceName) throws ReflectiveOperationException {
        Object metadata = invoke(cqlSession, METHOD_GET_METADATA);
        Class<?> cqlIdentifierClass = Class.forName(CQL_IDENTIFIER_CLASS);
        Object keyspaceIdentifier = cqlIdentifierClass.getMethod(METHOD_FROM_CQL, String.class).invoke(null, keyspaceName);

        Method getKeyspace = metadata.getClass().getMethod(METHOD_GET_KEYSPACE, cqlIdentifierClass);
        return unwrapOptional(getKeyspace.invoke(metadata, keyspaceIdentifier));
    }

    private static void execute(Object cqlSession, String cql) throws ReflectiveOperationException {
        cqlSession.getClass().getMethod(METHOD_EXECUTE, String.class).invoke(cqlSession, cql);
    }

    private static Object unwrapOptional(Object optional) throws ReflectiveOperationException {
        boolean present = (boolean) invoke(optional, METHOD_IS_PRESENT);
        return present ? invoke(optional, METHOD_GET) : null;
    }

    /**
     * Renders an identifier as it is to be written in CQL, ie quoted when it is not all lower case,
     * so that a table like {@code "MyTable"} is truncated rather than reported as missing.
     */
    private static String asCql(Object cqlIdentifier) throws ReflectiveOperationException {
        return (String) cqlIdentifier.getClass().getMethod(METHOD_AS_CQL, boolean.class).invoke(cqlIdentifier, true);
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }
}