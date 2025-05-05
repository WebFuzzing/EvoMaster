package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.heuristic.SqlBaseTableReference;
import org.evomaster.client.java.sql.heuristic.SqlColumnReference;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.evomaster.client.java.sql.internal.SqlParserUtils.*;

/**
 * Class used to act upon SQL commands executed by the SUT
 */
public class SqlHandler {

    private final static Set<String> booleanConstantNames = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("t", "true", "f", "false", "yes", "y", "no", "n", "on", "off", "unknown"))
    );

    private final TaintHandler taintHandler;

    /**
     * Computing heuristics on SQL is expensive, as we need to run
     * further queries. So, we buffer them, and execute them only
     * if needed (ie, lazy initialization)
     */
    private final List<SqlExecutionLogDto> bufferedSqlCommands;

    /**
     * The heuristics based on the SQL execution
     */
    private final List<SqlCommandWithDistance> distances;

    //see ExecutionDto
    private final Map<SqlTableId, Set<SqlColumnId>> queriedData;
    private final Map<SqlTableId, Set<SqlColumnId>> updatedData;
    private final Map<SqlTableId, Set<SqlColumnId>> insertedData;
    private final Map<SqlTableId, Set<SqlColumnId>> failedWhere;
    private final List<SqlTableId> deletedData;
    private final List<SqlExecutionLogDto> executedSqlCommands;

    private int numberOfSqlCommands;

    private int sqlParseFailureCount;

    private volatile Connection connection;

    private volatile boolean calculateHeuristics;

    private volatile boolean extractSqlExecution;

    private volatile boolean advancedHeuristics;

    /**
     * WARNING: in general we shouldn't use mutable DTO as internal data structures.
     * But, here, what we need is very simple (just checking for names).
     */
    private volatile DbInfoDto schema;

    public SqlHandler(TaintHandler taintHandler) {

        this.taintHandler = taintHandler;

        bufferedSqlCommands = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();
        queriedData = new ConcurrentHashMap<>();
        updatedData = new ConcurrentHashMap<>();
        insertedData = new ConcurrentHashMap<>();
        failedWhere = new ConcurrentHashMap<>();
        deletedData = new CopyOnWriteArrayList<>();
        executedSqlCommands = new CopyOnWriteArrayList<>();

        calculateHeuristics = true;
        numberOfSqlCommands = 0;
        sqlParseFailureCount = 0;
    }

    public void reset() {
        bufferedSqlCommands.clear();
        distances.clear();
        queriedData.clear();
        updatedData.clear();
        insertedData.clear();
        failedWhere.clear();
        deletedData.clear();
        executedSqlCommands.clear();

        numberOfSqlCommands = 0;
        sqlParseFailureCount = 0;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setSchema(DbInfoDto schema) {
        this.schema = schema;
    }

    /**
     * handle executed sql info
     *
     * @param sqlExecutionLogDto to be handled
     */
    public void handle(SqlExecutionLogDto sqlExecutionLogDto) {
        executedSqlCommands.add(sqlExecutionLogDto);
        final String sqlCommand = sqlExecutionLogDto.sqlCommand;
        Objects.requireNonNull(sqlCommand);

        if (!calculateHeuristics && !extractSqlExecution) {
            return;
        }

        numberOfSqlCommands++;

        if (!SqlParserUtils.canParseSqlStatement(sqlCommand)) {
            sqlParseFailureCount++;
            SimpleLogger.warn("Cannot parse SQL statement: " + sqlCommand);
            return;
        }

        // all SQL statements added to bufferedSqlCommands can be parsed
        bufferedSqlCommands.add(sqlExecutionLogDto);

        if (isSelect(sqlCommand)) {
            mergeNewData(queriedData, ColumnTableAnalyzer.getSelectReadDataFields(sqlCommand));
        } else if (isDelete(sqlCommand)) {
            deletedData.add(ColumnTableAnalyzer.getDeletedTable(sqlCommand));
        } else if (isInsert(sqlCommand)) {
            final Map.Entry<SqlTableId, Set<SqlColumnId>> insertedDataFields = ColumnTableAnalyzer.getInsertedDataFields(sqlCommand);
            mergeNewData(insertedData, Collections.singletonMap(insertedDataFields.getKey(), insertedDataFields.getValue()));
        } else if (isUpdate(sqlCommand)) {
            final Map.Entry<SqlTableId, Set<SqlColumnId>> updatedDataFields = ColumnTableAnalyzer.getUpdatedDataFields(sqlCommand);
            mergeNewData(updatedData, Collections.singletonMap(updatedDataFields.getKey(), updatedDataFields.getValue()));
        }

    }

    public SqlExecutionsDto getExecutionDto() {

        if (!calculateHeuristics && !extractSqlExecution) {
            return null;
        }

        SqlExecutionsDto sqlExecutionsDto = new SqlExecutionsDto();
        sqlExecutionsDto.queriedData.putAll(getTableToColumnsMap(queriedData));
        sqlExecutionsDto.failedWhere.putAll(getTableToColumnsMap(failedWhere));
        sqlExecutionsDto.insertedData.putAll(getTableToColumnsMap(insertedData));
        sqlExecutionsDto.updatedData.putAll(getTableToColumnsMap(updatedData));
        sqlExecutionsDto.deletedData.addAll(deletedData.stream().map(SqlTableId::getTableId).collect(Collectors.toSet()));
        sqlExecutionsDto.numberOfSqlCommands = this.numberOfSqlCommands;
        sqlExecutionsDto.sqlParseFailureCount = this.sqlParseFailureCount;
        sqlExecutionsDto.sqlExecutionLogDtoList.addAll(executedSqlCommands);
        return sqlExecutionsDto;
    }

    private static Map<String, Set<String>> getTableToColumnsMap(Map<SqlTableId, Set<SqlColumnId>> originalMap) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<SqlTableId, Set<SqlColumnId>> originalEntry : originalMap.entrySet()) {
            result.put(originalEntry.getKey().getTableId(), originalEntry.getValue().stream().map(SqlColumnId::getColumnId).collect(Collectors.toSet()));
        }
        return result;
    }

    /**
     * Check if the SQL command is valid for distance computation.
     */
    private boolean isValidSqlCommandForDistanceEvaluation(String sqlCommand) {
        return !isSelectOne(sqlCommand) &&
                (isSelect(sqlCommand) || isDelete(sqlCommand) || isUpdate(sqlCommand));
    }

    /**
     * compute (SELECT, DELETE and UPDATE) sql distance for sql commands which exists in [buffer]
     * Note that we skip `SELECT 1` (typically for testing sql connection) since its distance is 0
     *
     * @return a list of heuristics for sql commands
     */
    public List<SqlCommandWithDistance> getSqlDistances(List<InsertionDto> successfulInitSqlInsertions, boolean queryFromDatabase) {

        if (connection == null || !calculateHeuristics) {
            return distances;
        }

        // compute buffered Sql Commands and clear buffer
        if (!bufferedSqlCommands.isEmpty()) {
            bufferedSqlCommands.forEach(sqlExecutionLogDto -> {
                String sqlCommand = sqlExecutionLogDto.sqlCommand;

                if (sqlExecutionLogDto.threwSqlExeception != true
                        && isValidSqlCommandForDistanceEvaluation(sqlCommand)) {

                    /*
                     * All SQL commands that were saved to bufferedSqlCommands
                     * were previously parsed with SqlParserUtils.canParseSqlStatement().
                     * Therefore, we can assume that they can be successfully
                     * parsed again.
                     */
                    Statement parsedStatement = SqlParserUtils.parseSqlCommand(sqlCommand);
                    SqlDistanceWithMetrics sqlDistance = computeDistance(sqlCommand,
                            parsedStatement,
                            successfulInitSqlInsertions,
                            queryFromDatabase);
                    distances.add(new SqlCommandWithDistance(sqlCommand, sqlDistance));
                }
            });

            //side effects on buffer is not important, as it is just a cache
            bufferedSqlCommands.clear();
        }

        return distances;
    }

    private SqlDistanceWithMetrics computeDistance(String sqlCommand,
                                                   Statement parsedStatement,
                                                   List<InsertionDto> successfulInitSqlInsertions,
                                                   boolean queryFromDatabase) {

        if (connection == null) {
            throw new IllegalStateException("Trying to calculate SQL distance with no DB connection");
        }

        Map<SqlTableId, Set<SqlColumnId>> columns = extractColumnsInvolvedInWhere(parsedStatement);

        /*
            even if columns.isEmpty(), we need to check if any data was present
         */

        SqlDistanceWithMetrics dist;
        if (columns.isEmpty() || queryFromDatabase) {
            dist = getDistanceForWhere(sqlCommand, columns);
        } else {
            // !columns.isEmpty() && !queryFromDatabase
            dist = getDistanceForWhereBasedOnInsertion(sqlCommand, columns, successfulInitSqlInsertions);
        }

        if (dist.sqlDistance > 0) {
            mergeNewData(failedWhere, columns);
        }

        return dist;
    }


    private SqlDistanceWithMetrics getDistanceForWhereBasedOnInsertion(String sqlCommand, Map<SqlTableId, Set<SqlColumnId>> columns, List<InsertionDto> insertionDtos) {
        QueryResult[] data = QueryResultTransformer.convertInsertionDtosToQueryResults(insertionDtos, columns, schema);
        assert data != null;
        return HeuristicsCalculator.computeDistance(sqlCommand, schema, taintHandler, advancedHeuristics, data);
    }


    private SqlDistanceWithMetrics getDistanceForWhere(String sqlCommand, Map<SqlTableId, Set<SqlColumnId>> columns) {
        if (!isSelect(sqlCommand) && !isDelete(sqlCommand) && !isUpdate(sqlCommand)) {
            throw new IllegalArgumentException("Cannot compute distance for sql command: " + sqlCommand);
        }
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema, booleanConstantNames);
        Statement statement = SqlParserUtils.parseSqlCommand(sqlCommand);
        statement.accept(finder);

        final Map<SqlBaseTableReference, Set<SqlColumnReference>> columnsInWhere = finder.getColumnReferences();
        List<QueryResult> queryResults = new ArrayList<>();
        for (SqlBaseTableReference sqlBaseTableReference : columnsInWhere.keySet()) {
            SqlTableId tableId = new SqlTableId(sqlBaseTableReference.getFullyQualifiedName());
            Set<SqlColumnId> columnIds = columnsInWhere.get(sqlBaseTableReference).stream()
                    .map(SqlColumnReference::getColumnName)
                    .map(SqlColumnId::new)
                    .collect(Collectors.toSet());
            String select = createSelectForSingleTable(tableId, columnIds);
            try {
                QueryResult queryResult = SqlScriptRunner.execCommand(connection, select);
                queryResults.add(queryResult);
            } catch (SQLException e) {
                SimpleLogger.uniqueWarn("Failed to execute query for retrieving data for computing SQL heuristics: " + select);
                return new SqlDistanceWithMetrics(Double.MAX_VALUE, 0, true);
            }
        }
        return HeuristicsCalculator.computeDistance(sqlCommand, schema, taintHandler, advancedHeuristics, queryResults.toArray(new QueryResult[0]));
    }

    private String createSelectForSingleTable(SqlTableId tableId, Set<SqlColumnId> columnIds) {

        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ");

        String variables = columnIds.stream().map(SqlColumnId::getColumnId).collect(Collectors.joining(", "));

        buffer.append(variables);
        buffer.append(" FROM ");
        buffer.append(tableId.getTableId());

        return buffer.toString();
    }

    /**
     * Check the fields involved in the WHERE clause (if any).
     * Return a map from table name to column names of the involved fields.
     */
    public Map<SqlTableId, Set<SqlColumnId>> extractColumnsInvolvedInWhere(Statement statement) {

        /*
           TODO
           following does not handle the case of sub-selects involving other
           tables... but likely that is not something we need to support right now
         */

        Map<SqlTableId, Set<SqlColumnId>> result = new HashMap<>();

        // move getWhere before SqlNameContext, otherwise null where would cause exception in new SqlNameContext
        Expression where = SqlParserUtils.getWhere(statement);
        if (where == null) {
            return result;
        }

        SqlNameContext context = new SqlNameContext(statement);
        if (schema != null) {
            context.setSchema(schema);
        }

        ExpressionVisitor visitor = new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column column) {

                String tableName = context.getTableName(column);

                if (tableName.equalsIgnoreCase(SqlNameContext.UNNAMED_TABLE)) {
                    // TODO handle it properly when ll have support for sub-selects
                    return;
                }

                String columnName = column.getColumnName().toLowerCase();

                if (!context.hasColumn(tableName, columnName)) {

                    /*
                        This is an issue with the JsqlParser library. Until we upgrade it, or fix it if not fixed yet,
                        we use this workaround.

                        The problem is that some SQL databases do not have support for boolean types, so parser can
                        interpret constants like TRUE as column names.
                        And all databases have differences on how booleans are treated, eg.
                        - H2: TRUE, FALSE, and UNKNOWN (NULL).
                          http://www.h2database.com/html/datatypes.html#boolean_type
                        - Postgres:  true, yes, on, 1, false, no, off, 0 (as well as abbreviations like t and f)
                          https://www.postgresql.org/docs/9.5/datatype-boolean.html

                     */

                    if (booleanConstantNames.contains(columnName)) {
                        //case in which a boolean constant is wrongly treated as a column name.
                        //TODO not sure what we can really do here without modifying the parser
                    } else {
                        SimpleLogger.warn("Cannot find column '" + columnName + "' in table '" + tableName + "'");
                    }
                    return;
                }

                result.putIfAbsent(new SqlTableId(tableName), new HashSet<>());
                Set<SqlColumnId> columnIds = result.get(new SqlTableId(tableName));
                columnIds.add(new SqlColumnId(columnName));
            }
        };

        where.accept(visitor);

        return result;
    }

    private static void mergeNewData(
            Map<SqlTableId, Set<SqlColumnId>> current,
            Map<SqlTableId, Set<SqlColumnId>> toAdd
    ) {

        for (Map.Entry<SqlTableId, Set<SqlColumnId>> e : toAdd.entrySet()) {
            SqlTableId key = e.getKey();
            Set<SqlColumnId> values = e.getValue();

            Set<SqlColumnId> existing = current.get(key);

            if (existing != null && existing.contains(new SqlColumnId("*"))) {
                //nothing to do
                continue;
            }

            if (existing == null) {
                existing = new HashSet<>(values);
                current.put(key, existing);
            } else {
                existing.addAll(values);
            }

            if (existing.size() > 1 && existing.contains(new SqlColumnId("*"))) {
                /*
                    remove unnecessary columns, as anyway we take
                    everything with *
                 */
                existing.clear();
                existing.add(new SqlColumnId("*"));
            }
        }
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public boolean isExtractSqlExecution() {
        return extractSqlExecution;
    }

    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    public void setExtractSqlExecution(boolean extractSqlExecution) {
        this.extractSqlExecution = extractSqlExecution;
    }

    public boolean isAdvancedHeuristics() {
        return advancedHeuristics;
    }

    public void setAdvancedHeuristics(boolean advancedHeuristics) {
        this.advancedHeuristics = advancedHeuristics;
    }
}
