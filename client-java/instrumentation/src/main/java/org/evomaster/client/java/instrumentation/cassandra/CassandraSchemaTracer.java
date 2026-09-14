package org.evomaster.client.java.instrumentation.cassandra;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches Cassandra table schemas read directly from the driver's own metadata
 * ({@code CqlSession.getMetadata()}), rather than from Spring Data hooks. This works regardless of
 * whether the SUT talks to Cassandra through Spring Data repositories, {@code CassandraTemplate},
 * or a bare {@code CqlSession}, since the driver always carries this information.
 * <p>
 * {@link #resolve(Object, String, String)} is called from
 * {@link org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.CqlSessionClassReplacement}
 * right after every successfully executed CQL statement, so the cache is populated as a side effect
 * of real SUT traffic: a table's shape is fetched once, the first time it's referenced, traced via
 * {@link ExecutionTracer#addCassandraTableMetadata(CassandraTableMetadata)}, and served straight
 * from the cache (without re-tracing it) on every later reference, since {@code TableMetadata} is
 * never partial (a single lookup already returns every column of a table).
 * <p>
 * The cache is held in a static field rather than an injected instance: it mirrors
 * {@link org.evomaster.client.java.instrumentation.object.ClassToSchema}'s cache, and needs to be
 * reachable from the static replacement methods on {@code CqlSessionClassReplacement}.
 */
public class CassandraSchemaTracer {

    private static final String CQL_IDENTIFIER_CLASS = "com.datastax.oss.driver.api.core.CqlIdentifier";

    /*
     * Names of the driver methods invoked via reflection throughout this class.
     */
    private static final String METHOD_GET_METADATA = "getMetadata";
    private static final String METHOD_GET_KEYSPACE = "getKeyspace";
    private static final String METHOD_GET_TABLE = "getTable";
    private static final String METHOD_GET_NAME = "getName";
    private static final String METHOD_GET_TYPE = "getType";
    private static final String METHOD_FROM_CQL = "fromCql";
    private static final String METHOD_IS_PRESENT = "isPresent";
    private static final String METHOD_GET = "get";
    private static final String METHOD_AS_INTERNAL = "asInternal";
    private static final String METHOD_AS_CQL = "asCql";
    private static final String METHOD_GET_PARTITION_KEY = "getPartitionKey";
    private static final String METHOD_GET_CLUSTERING_COLUMNS = "getClusteringColumns";
    private static final String METHOD_GET_COLUMNS = "getColumns";

    /**
     * Key -> the captured shape of a keyspace's table.
     * Value -> its columns, types, and primary-key roles, as last read from the driver.
     */
    private static final Map<TableKey, CassandraTableMetadata> tables = new ConcurrentHashMap<>();

    private CassandraSchemaTracer() {
    }

    /**
     * Clears the cached table shapes. Called once per new search (from
     * {@link org.evomaster.client.java.instrumentation.InstrumentationController#resetForNewSearch()}),
     * not per test.
     */
    public static void reset() {
        tables.clear();
    }

    /**
     * Resolves the actual keyspace name a CQL statement targets: the driver-canonical form of
     * the explicit qualifier, if present in the query text, otherwise the session's current
     * default keyspace.
     *
     * @param cqlSession      a live {@code com.datastax.oss.driver.api.core.CqlSession}, accessed
     *                        purely via reflection so this module has no compile-time driver dependency
     * @param keyspaceNameRaw the raw, quote-preserving keyspace text as it appeared in the CQL
     *                        statement, or {@code null} if the statement didn't qualify the table
     *                        with a keyspace (the session's current keyspace applies instead)
     * @return the resolved keyspace name, in the driver's canonical/internal form, or {@code null}
     *         if it can't be resolved (unqualified statement with no current keyspace set on the
     *         session, or an explicit keyspace qualifier that doesn't exist)
     */
    public static String resolveKeyspaceName(Object cqlSession, String keyspaceNameRaw) {
        try {
            Object keyspaceMetadata = resolveKeyspaceMetadata(cqlSession, keyspaceNameRaw);
            return keyspaceMetadata != null ? asInternal(invoke(keyspaceMetadata, METHOD_GET_NAME)) : null;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to resolve Cassandra keyspace name for " + keyspaceNameRaw, e);
        }
    }

    /**
     * Resolves the shape of a keyspace/table. Tables seen before are served straight from the
     * cache; a table not seen before (e.g. queried for the first time) is fetched once and cached.
     *
     * @param cqlSession      a live {@code com.datastax.oss.driver.api.core.CqlSession}, accessed
     *                        purely via reflection so this module has no compile-time driver dependency
     * @param keyspaceNameRaw the raw, quote-preserving keyspace text as it appeared in the CQL
     *                        statement, or {@code null} if the statement didn't qualify the table
     *                        with a keyspace (the session's current keyspace applies instead)
     * @param tableNameRaw    the raw, quote-preserving table text as it appeared in the CQL statement
     * @return the table's shape, or {@code null} if the keyspace/table can't be resolved
     *         (unqualified reference with no current keyspace set, or the keyspace/table doesn't exist)
     */
    public static CassandraTableMetadata resolve(Object cqlSession, String keyspaceNameRaw, String tableNameRaw) {
        try {
            Object keyspaceMetadata = resolveKeyspaceMetadata(cqlSession, keyspaceNameRaw);
            if (keyspaceMetadata == null) {
                return null;
            }

            Object tableIdentifier = cqlIdentifierFromCql(tableNameRaw);
            Object tableMetadataObj = unwrapOptional(invokeWithCqlIdentifier(keyspaceMetadata, METHOD_GET_TABLE, tableIdentifier));
            if (tableMetadataObj == null) {
                return null;
            }

            String keyspaceName = asInternal(invoke(keyspaceMetadata, METHOD_GET_NAME));
            String tableName = asInternal(invoke(tableMetadataObj, METHOD_GET_NAME));
            TableKey key = new TableKey(keyspaceName, tableName);

            // fast path: skip re-walking columns/partition-key/clustering-columns on a cache hit,
            // since this is called on every intercepted query
            CassandraTableMetadata cached = tables.get(key);
            if (cached != null) {
                return cached;
            }

            CassandraTableMetadata tableMetadata = buildTableMetadata(keyspaceName, tableMetadataObj);
            tables.put(key, tableMetadata);
            // first time this table is seen: trace its schema
            ExecutionTracer.addCassandraTableMetadata(tableMetadata);
            return tableMetadata;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to resolve Cassandra table metadata for "
                    + keyspaceNameRaw + "." + tableNameRaw, e);
        }
    }

    private static CassandraTableMetadata buildTableMetadata(String keyspaceName, Object tableMetadataObj) throws ReflectiveOperationException {
        String tableName = asInternal(invoke(tableMetadataObj, METHOD_GET_NAME));

        Set<String> partitionKeyNames = namesOf((List<?>) invoke(tableMetadataObj, METHOD_GET_PARTITION_KEY));
        Set<String> clusteringNames = namesOf(((Map<?, ?>) invoke(tableMetadataObj, METHOD_GET_CLUSTERING_COLUMNS)).keySet());

        Map<?, ?> columnsByName = (Map<?, ?>) invoke(tableMetadataObj, METHOD_GET_COLUMNS);
        List<CassandraColumnMetadata> columns = new ArrayList<>();
        for (Object columnMetadataObj : columnsByName.values()) {
            String columnName = asInternal(invoke(columnMetadataObj, METHOD_GET_NAME));
            String cqlType = asCql(invoke(columnMetadataObj, METHOD_GET_TYPE));
            columns.add(new CassandraColumnMetadata(columnName, cqlType,
                    partitionKeyNames.contains(columnName), clusteringNames.contains(columnName)));
        }
        return new CassandraTableMetadata(keyspaceName, tableName, columns);
    }

    private static Set<String> namesOf(Iterable<?> columnMetadataObjs) throws ReflectiveOperationException {
        Set<String> names = new HashSet<>();
        for (Object columnMetadataObj : columnMetadataObjs) {
            names.add(asInternal(invoke(columnMetadataObj, METHOD_GET_NAME)));
        }
        return names;
    }

    /**
     * Resolves the driver's own {@code KeyspaceMetadata} for a statement's keyspace: the explicit
     * qualifier, if present, otherwise the session's current default keyspace. Shared by
     * {@link #resolve(Object, String, String)} and {@link #resolveKeyspaceName(Object, String)},
     * the two entry points that need to walk from a raw keyspace qualifier (or its absence) down
     * to the driver's resolved keyspace.
     */
    private static Object resolveKeyspaceMetadata(Object cqlSession, String keyspaceNameRaw) throws ReflectiveOperationException {
        Object keyspaceIdentifier = keyspaceNameRaw != null
                ? cqlIdentifierFromCql(keyspaceNameRaw)
                : currentKeyspaceIdentifier(cqlSession);
        if (keyspaceIdentifier == null) {
            return null;
        }

        Object metadata = invoke(cqlSession, METHOD_GET_METADATA);
        return unwrapOptional(invokeWithCqlIdentifier(metadata, METHOD_GET_KEYSPACE, keyspaceIdentifier));
    }

    private static Object currentKeyspaceIdentifier(Object cqlSession) throws ReflectiveOperationException {
        return unwrapOptional(invoke(cqlSession, METHOD_GET_KEYSPACE));
    }

    private static Object cqlIdentifierFromCql(String rawText) throws ReflectiveOperationException {
        Class<?> cqlIdentifierClass = Class.forName(CQL_IDENTIFIER_CLASS);
        return cqlIdentifierClass.getMethod(METHOD_FROM_CQL, String.class).invoke(null, rawText);
    }

    private static Object invokeWithCqlIdentifier(Object target, String methodName, Object cqlIdentifierArg) throws ReflectiveOperationException {
        Class<?> cqlIdentifierClass = Class.forName(CQL_IDENTIFIER_CLASS);
        Method method = target.getClass().getMethod(methodName, cqlIdentifierClass);
        return method.invoke(target, cqlIdentifierArg);
    }

    private static Object unwrapOptional(Object optional) throws ReflectiveOperationException {
        boolean present = (boolean) invoke(optional, METHOD_IS_PRESENT);
        return present ? invoke(optional, METHOD_GET) : null;
    }

    private static String asInternal(Object cqlIdentifier) throws ReflectiveOperationException {
        return (String) invoke(cqlIdentifier, METHOD_AS_INTERNAL);
    }

    private static String asCql(Object dataType) throws ReflectiveOperationException {
        return (String) dataType.getClass().getMethod(METHOD_AS_CQL, boolean.class, boolean.class).invoke(dataType, false, false);
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }
}