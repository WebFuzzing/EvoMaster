package org.evomaster.client.java.controller.internal.db.cassandra;

import org.evomaster.client.java.controller.api.dto.database.execution.CassandraExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.CassandraFailedQuery;
import org.evomaster.client.java.controller.cassandra.calculator.CassandraHeuristicsCalculator;
import org.evomaster.client.java.controller.cassandra.model.CassandraRow;
import org.evomaster.client.java.controller.cassandra.model.CqlTableReference;
import org.evomaster.client.java.controller.cassandra.parser.CqlParserUtils;
import org.evomaster.client.java.instrumentation.ExecutedCqlCommand;
import org.evomaster.client.java.instrumentation.cassandra.CassandraColumnMetadata;
import org.evomaster.client.java.instrumentation.cassandra.CassandraSchemaTracer;
import org.evomaster.client.java.instrumentation.cassandra.CassandraTableMetadata;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Acts upon CQL commands executed by the SUT against a Cassandra database. It buffers the
 * SELECT/UPDATE/DELETE commands intercepted via instrumentation and, on demand, computes a
 * heuristic distance for each: the target table's full contents are re-fetched (via reflection
 * against the live {@code CqlSession}, so no compile-time dependency on the Cassandra driver is
 * needed) and evaluated with {@link CassandraHeuristicsCalculator}, to guide the search towards
 * making the WHERE clause satisfiable.
 */
public class CassandraHandler {

    /**
     * Fully-qualified name of the Cassandra driver's session interface, used to locate it via
     * reflection on the live session object without a compile-time dependency on the driver.
     */
    public static final String SESSION_INTERFACE = "com.datastax.oss.driver.api.core.CqlSession";

    /*
        Names of the driver methods invoked via reflection throughout this class.
     */
    public static final String METHOD_EXECUTE = "execute";
    public static final String METHOD_GET_COLUMN_DEFINITIONS = "getColumnDefinitions";
    public static final String METHOD_GET_NAME = "getName";
    public static final String METHOD_AS_INTERNAL = "asInternal";
    public static final String METHOD_GET_OBJECT = "getObject";

    /*
        Constants used during schema description
     */
    public static final String PARTITION_KEY_COLUMN_SUFFIX = " PARTITION KEY";
    public static final String CLUSTERING_COLUMN_SUFFIX = " CLUSTERING";


    public static final String SELECT_ALL_PREFIX = "SELECT * FROM ";

    /**
     * Live CqlSession from the SUT. Held as {@code Object} to avoid a hard compile-time
     * dependency on the Cassandra driver.
     */
    private Object cqlSession = null;

    /**
     * CQL SELECT/UPDATE/DELETE commands captured this action, awaiting distance computation.
     */
    private final List<ExecutedCqlCommand> operations = new ArrayList<>();

    private final List<CqlCommandWithDistance> cqlCommandWithDistances = new ArrayList<>();

    /**
     * CQL commands whose target table came back empty when their distance was computed, kept as
     * a data-generation hint.
     */
    private final List<ExecutedCqlCommand> emptyTableQueries = new ArrayList<>();

    /**
     * Table name to column schema, populated from {@link CassandraTableMetadata} instances read
     * directly off the Cassandra driver's own metadata (see {@link CassandraSchemaTracer}), the
     * first time a query references that table.
     */
    private final Map<String, CassandraTableMetadata> tableSchemas = new HashMap<>();

    private volatile boolean extractCqlExecution = true;

    private volatile boolean calculateHeuristics = true;

    private final CassandraHeuristicsCalculator calculator = new CassandraHeuristicsCalculator();

    /**
     * Clears the CQL commands and empty-table queries buffered for the current action, along
     * with their computed distances. Table schemas are not cleared: each table's schema is
     * captured once, the first time it's queried, and does not change from one action to the next.
     */
    public void reset() {
        operations.clear();
        cqlCommandWithDistances.clear();
        emptyTableQueries.clear();
        // tableSchemas is not cleared: each table's schema is captured once, the first time it's queried
    }

    public void setCqlSession(Object cqlSession) {
        this.cqlSession = cqlSession;
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public boolean isExtractCqlExecution() {
        return extractCqlExecution;
    }

    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    public void setExtractCqlExecution(boolean extractCqlExecution) {
        this.extractCqlExecution = extractCqlExecution;
    }

    /**
     * Buffers an intercepted CQL execution. Only SELECT, UPDATE and DELETE are kept: those are
     * the only statements with a WHERE clause whose satisfiability is worth guiding the search
     * towards (an INSERT/DDL command has no WHERE to compute a distance against).
     */
    public void handle(ExecutedCqlCommand info) {
        Objects.requireNonNull(info);
        Objects.requireNonNull(info.getCqlCommand());

        if (extractCqlExecution && isDistanceEvaluable(info.getCqlCommand())) {
            operations.add(info);
        }
    }

    /**
     * Records a table's column schema, read directly off the Cassandra driver's own metadata the
     * first time a query references that table (see {@link CassandraSchemaTracer}), so it can
     * later be attached to empty-table hints in {@link #getExecutionDto()}.
     */
    public void handle(CassandraTableMetadata info) {
        Objects.requireNonNull(info);

        if (extractCqlExecution) {
            tableSchemas.put(info.getTableName(), info);
        }
    }

    private static boolean isDistanceEvaluable(String cqlCommand) {
        return CqlParserUtils.isSelect(cqlCommand)
                || CqlParserUtils.isUpdate(cqlCommand)
                || CqlParserUtils.isDelete(cqlCommand);
    }

    /**
     * Computes the heuristic distance for every buffered CQL command, draining the buffer in the
     * process, and returns the accumulated results across all rounds since the last
     * {@link #reset()}.
     */
    public List<CqlCommandWithDistance> getEvaluatedCqlCommands() {
        operations.forEach(info -> {
            CqlDistanceWithMetrics distance = computeQueryDistance(info);
            cqlCommandWithDistances.add(new CqlCommandWithDistance(info.getCqlCommand(), distance));
        });
        operations.clear();

        return cqlCommandWithDistances;
    }

    /**
     * @return the CQL commands whose target table was found empty, as a data-generation hint.
     */
    public CassandraExecutionsDto getExecutionDto() {
        CassandraExecutionsDto dto = new CassandraExecutionsDto();
        dto.failedQueries = emptyTableQueries.stream()
                .map(this::extractRelevantInfo)
                .collect(Collectors.toList());

        return dto;
    }

    private CassandraFailedQuery extractRelevantInfo(ExecutedCqlCommand info) {
        CassandraTableMetadata schema = tableSchemas.get(info.getTableName());
        String tableSchema = schema != null ? describeTableSchema(schema) : null;

        return new CassandraFailedQuery(info.getKeyspaceName(), info.getTableName(), tableSchema);
    }

    /**
     * Renders a table's columns as {@code name type [PARTITION KEY|CLUSTERING]} entries, so the
     * schema can be attached to a {@link CassandraFailedQuery} (a plain string, to keep
     * controller-api free of a dependency on the instrumentation module's
     * {@link CassandraTableMetadata}).
     */
    private static String describeTableSchema(CassandraTableMetadata schema) {
        return schema.getColumns().stream()
                .map(CassandraHandler::describeColumn)
                .collect(Collectors.joining(", "));
    }

    private static String describeColumn(CassandraColumnMetadata column) {
        StringBuilder description = new StringBuilder(column.getName()).append(' ').append(column.getCqlType());
        if (column.isPartitionKey()) {
            description.append(PARTITION_KEY_COLUMN_SUFFIX);
        }
        if (column.isClusteringColumn()) {
            description.append(CLUSTERING_COLUMN_SUFFIX);
        }
        return description.toString();
    }

    private CqlDistanceWithMetrics computeQueryDistance(ExecutedCqlCommand info) {
        if (cqlSession == null) {
            throw new IllegalStateException("Trying to calculate CQL distance with no CqlSession set");
        }

        String cql = info.getCqlCommand();

        CqlTableReference tableReference = resolveTableReference(cql);
        if (tableReference == null) {
            SimpleLogger.uniqueWarn("Could not determine the target table of CQL command: " + cql);

            return new CqlDistanceWithMetrics(Double.MAX_VALUE, 0);
        }

        List<CassandraRow> rows;
        try {
            rows = fetchAllRows(tableReference);
        } catch (Exception e) {
            SimpleLogger.uniqueWarn("Failed to fetch rows for computing CQL distance: " + cql);

            return new CqlDistanceWithMetrics(Double.MAX_VALUE, 0);
        }

        if (rows.isEmpty()) {
            emptyTableQueries.add(info);
        }

        double distance;
        try {
            distance = calculator.computeDistance(cql, rows);
        } catch (Exception e) {
            SimpleLogger.uniqueWarn("Failed to compute CQL distance for: " + cql);
            distance = Double.MAX_VALUE;
        }

        return new CqlDistanceWithMetrics(distance, rows.size());
    }

    private static CqlTableReference resolveTableReference(String cql) {
        try {
            return CqlParserUtils.getTableReference(CqlParserUtils.parseCqlCommand(cql));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Executes a full {@code SELECT * FROM keyspace.table} against the live session (via
     * reflection) and converts every returned row into a {@link CassandraRow}.
     * <p>
     * The query is issued with {@link ExecutionTracer#setExecutingInitCassandra} set, so that
     * this housekeeping read against the SUT's own {@code CqlSession} is not itself recorded as
     * a SUT-issued {@link ExecutedCqlCommand} (it would otherwise be picked up and re-evaluated
     * on a later round, polluting the reported heuristics).
     */
    private List<CassandraRow> fetchAllRows(CqlTableReference tableReference) throws Exception {
        String selectAll = buildSelectAll(tableReference);

        Object resultSet;
        ExecutionTracer.setExecutingInitCassandra(true);
        try {
            Method execute = detectSessionInterface().getMethod(METHOD_EXECUTE, String.class);
            resultSet = execute.invoke(cqlSession, selectAll);
        } finally {
            ExecutionTracer.setExecutingInitCassandra(false);
        }

        List<String> columnNames = extractColumnNames(resultSet);
        return buildRows(resultSet, columnNames);
    }

    private static String buildSelectAll(CqlTableReference tableReference) {
        String keyspaceName = tableReference.getKeyspaceName();
        String maybeKeyspace = keyspaceName != null ? keyspaceName + "." : "";

        return SELECT_ALL_PREFIX + maybeKeyspace + tableReference.getTableName();
    }

    private Class<?> detectSessionInterface() throws ClassNotFoundException {
        return Arrays.stream(cqlSession.getClass().getInterfaces())
                .filter(i -> i.getName().equals(SESSION_INTERFACE))
                .findFirst()
                .orElseThrow(() -> new ClassNotFoundException(
                        "CqlSession interface not found on " + cqlSession.getClass().getName()));
    }

    private static List<String> extractColumnNames(Object resultSet) throws ReflectiveOperationException {
        Method getColumnDefinitions = resultSet.getClass().getMethod(METHOD_GET_COLUMN_DEFINITIONS);
        Iterable<?> columnDefinitions = (Iterable<?>) getColumnDefinitions.invoke(resultSet);

        List<String> names = new ArrayList<>();
        for (Object columnDefinition : columnDefinitions) {
            Method getName = columnDefinition.getClass().getMethod(METHOD_GET_NAME);
            Object cqlIdentifier = getName.invoke(columnDefinition);
            Method asInternal = cqlIdentifier.getClass().getMethod(METHOD_AS_INTERNAL);
            names.add((String) asInternal.invoke(cqlIdentifier));
        }

        return names;
    }

    private static List<CassandraRow> buildRows(Object resultSet, List<String> columnNames) throws ReflectiveOperationException {
        List<CassandraRow> rows = new ArrayList<>();

        for (Object row : (Iterable<?>) resultSet) {
            rows.add(new CassandraRow(rowToMap(row, columnNames)));
        }

        return rows;
    }

    private static Map<String, Object> rowToMap(Object row, List<String> columnNames) throws ReflectiveOperationException {
        Method getObject = row.getClass().getMethod(METHOD_GET_OBJECT, int.class);
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < columnNames.size(); i++) {
            map.put(columnNames.get(i), getObject.invoke(row, i));
        }

        return map;
    }
}