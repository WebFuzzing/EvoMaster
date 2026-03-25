package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.heuristic.*;
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

    private volatile boolean completeSqlHeuristics;

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

        if (this.completeSqlHeuristics) {
            mergeNewDataForCompleteSqlHeuristics(sqlCommand);
        } else {
            mergeNewDataForPartialSqlHeuristics(sqlCommand);
        }

    }

    private void mergeNewDataForCompleteSqlHeuristics(String sqlCommand) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        if (parsedSqlCommand instanceof Select
                || parsedSqlCommand instanceof Delete
                || parsedSqlCommand instanceof Insert
                || parsedSqlCommand instanceof Update) {

            Map<SqlTableId, Set<SqlColumnId>> columns = extractColumnsInvolvedInStatement(parsedSqlCommand);
            if (parsedSqlCommand instanceof Select) {
                mergeNewData(queriedData, columns);
            } else if (parsedSqlCommand instanceof Delete) {
                deletedData.addAll(columns.keySet());
            } else if (parsedSqlCommand instanceof Insert) {
                mergeNewData(insertedData, columns);
            } else if (parsedSqlCommand instanceof Update) {
                mergeNewData(updatedData, columns);
            }
        }
    }


    private void mergeNewDataForPartialSqlHeuristics(String sqlCommand) {
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
        sqlExecutionsDto.deletedData.addAll(deletedData.stream().map(SqlTableId::getTableName).collect(Collectors.toSet()));
        sqlExecutionsDto.numberOfSqlCommands = this.numberOfSqlCommands;
        sqlExecutionsDto.sqlParseFailureCount = this.sqlParseFailureCount;
        sqlExecutionsDto.sqlExecutionLogDtoList.addAll(executedSqlCommands);
        return sqlExecutionsDto;
    }

    private static Map<String, Set<String>> getTableToColumnsMap(Map<SqlTableId, Set<SqlColumnId>> originalMap) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<SqlTableId, Set<SqlColumnId>> originalEntry : originalMap.entrySet()) {
            result.put(originalEntry.getKey().getTableName(), originalEntry.getValue().stream().map(SqlColumnId::getColumnId).collect(Collectors.toSet()));
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

                if (sqlExecutionLogDto.threwSqlExeception == false
                        && (this.completeSqlHeuristics
                        ? SqlHeuristicsCalculator.isValidSqlCommandForSqlHeuristicsCalculation(sqlCommand)
                        : isValidSqlCommandForDistanceEvaluation(sqlCommand))) {

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

        SqlDistanceWithMetrics dist;
        final Map<SqlTableId, Set<SqlColumnId>> columns;
        if (this.completeSqlHeuristics) {
            // advanced
            columns = extractColumnsInvolvedInStatement(parsedStatement);
            dist = computeCompleteSqlDistance(sqlCommand, parsedStatement, successfulInitSqlInsertions, queryFromDatabase, columns);
        } else {
            columns = extractColumnsInvolvedInWhere(parsedStatement);
            dist = computePartialSqlDistance(sqlCommand, parsedStatement, successfulInitSqlInsertions, queryFromDatabase);
        }
        if (dist.sqlDistance > 0) {
            mergeNewData(failedWhere, columns);
        }

        return dist;

    }

    private SqlDistanceWithMetrics computeCompleteSqlDistance(String sqlCommand,
                                                              Statement parsedStatement,
                                                              List<InsertionDto> successfulInitSqlInsertions,
                                                              boolean queryFromDatabase,
                                                              Map<SqlTableId, Set<SqlColumnId>> columns) {

        if (!SqlHeuristicsCalculator.isValidSqlCommandForSqlHeuristicsCalculation(parsedStatement)) {
            throw new IllegalArgumentException("Cannot compute distance for sql command: " + sqlCommand);
        }

        // fetch data for computing distances
        final QueryResultSet queryResultSet;

        if (queryFromDatabase) {
            try {
                List<QueryResult> queryResults = getQueryResultsForComputingSqlDistance(columns);
                queryResultSet = new QueryResultSet();
                for (QueryResult queryResult : queryResults) {
                    queryResultSet.addQueryResult(queryResult);
                }
            } catch (SQLException e) {
                SimpleLogger.uniqueWarn("Failed to execute query for retrieving data for computing SQL heuristics: " + sqlCommand);
                return new SqlDistanceWithMetrics(Double.MAX_VALUE, 0, true);
            }
        } else {
            // use the data from the successful insertions
            queryResultSet = QueryResultTransformer.translateInsertionDtos(successfulInitSqlInsertions, columns, schema);
        }

        // compute the SQL heuristics using the fetched data
        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator sqlHeuristicsCalculator = builder
                .withTableColumnResolver(new TableColumnResolver(schema))
                .withTaintHandler(taintHandler)
                .withSourceQueryResultSet(queryResultSet)
                .build();

        SqlDistanceWithMetrics sqlDistanceWithMetrics = sqlHeuristicsCalculator.computeDistance(sqlCommand);

        return sqlDistanceWithMetrics;
    }

    private Map<SqlTableId, Set<SqlColumnId>> extractColumnsInvolvedInStatement(Statement statement) {
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        statement.accept(finder);

        Map<SqlTableId, Set<SqlColumnId>> columns = new HashMap<>();
        for (SqlBaseTableReference baseTableReference : finder.getBaseTableReferences()) {
            SqlTableId tableId = baseTableReference.getTableId();
            if (finder.containsColumnReferences(baseTableReference)) {
                Set<SqlColumnId> columnIds = finder.getColumnReferences(baseTableReference).stream()
                        .map(SqlColumnReference::getColumnName)
                        .map(SqlColumnId::new)
                        .collect(Collectors.toSet());
                columns.put(tableId, columnIds);
            } else {
                columns.put(tableId, new LinkedHashSet<>());
            }
        }
        return columns;
    }

    private List<QueryResult> getQueryResultsForComputingSqlDistance(final Map<SqlTableId, Set<SqlColumnId>> columnsInWhere) throws SQLException {
        List<QueryResult> queryResults = new ArrayList<>();
        // we sort the table and column identifiers to improve testeability (i.e. allow mocking)
        for (SqlTableId tableId : columnsInWhere.keySet()
                .stream()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList())) {
            List<SqlColumnId> columnIds = columnsInWhere.get(tableId).stream()
                    .sorted(Comparator.comparing(Object::toString))
                    .collect(Collectors.toList());
            final String select;
            if (columnIds.isEmpty()) {
                // the table is required but no specific column was required.
                // Therefore, we need to fetch all columns for DELETE and UPDATE.
                select = SqlSelectBuilder.buildSelect(schema.databaseType, tableId, Collections.singletonList(new SqlColumnId("*")));
            } else {
                select = SqlSelectBuilder.buildSelect(schema.databaseType, tableId, columnIds);
            }
            QueryResult queryResult = SqlScriptRunner.execCommand(connection, select);
            queryResults.add(queryResult);
        }
        return queryResults;
    }

    private SqlDistanceWithMetrics computePartialSqlDistance(
            String sqlCommand,
            Statement parsedStatement,
            List<InsertionDto> successfulInitSqlInsertions,
            boolean queryFromDatabase) {

        final Map<SqlTableId, Set<SqlColumnId>> columns = extractColumnsInvolvedInWhere(parsedStatement);

        /*
            even if columns.isEmpty(), we need to check if any data was present
         */
        final SqlDistanceWithMetrics dist;
        if (columns.isEmpty() || queryFromDatabase) {
            dist = getDistanceForWhere(sqlCommand, columns);
        } else {
            // !columns.isEmpty() && !queryFromDatabase
            dist = getDistanceForWhereBasedOnInsertion(sqlCommand, columns, successfulInitSqlInsertions);
        }
        return dist;
    }


    private SqlDistanceWithMetrics getDistanceForWhereBasedOnInsertion(String sqlCommand, Map<SqlTableId, Set<SqlColumnId>> columns, List<InsertionDto> insertionDtos) {
        QueryResult[] data = QueryResultTransformer.convertInsertionDtosToQueryResults(insertionDtos, columns, schema);
        assert data != null;
        return HeuristicsCalculator.computeDistance(sqlCommand, schema, taintHandler, data);
    }


    private SqlDistanceWithMetrics getDistanceForWhere(String sqlCommand, Map<SqlTableId, Set<SqlColumnId>> columns) {
        if (!isSelect(sqlCommand) && !isDelete(sqlCommand) && !isUpdate(sqlCommand)) {
            throw new IllegalArgumentException("Cannot compute distance for sql command: " + sqlCommand);
        }
        String select;

        /*
           TODO:
           this might be likely unnecessary... we are only interested in the variables used
           in the WHERE. Furthermore, this would not support DELETE/INSERT/UPDATE.
           So, we just need to create a new SELECT based on that.
           But SELECT could be complex with many JOINs... whereas DIP would be simple(r)?

           TODO: we need a general solution
         */
        if (isSelect(sqlCommand)) {
            select = SelectTransformer.addFieldsToSelect(sqlCommand);
            select = SelectTransformer.removeConstraints(select);
            select = SelectTransformer.removeOperations(select);
        } else {
            if (columns.size() > 1) {
                SimpleLogger.uniqueWarn("Cannot analyze: " + sqlCommand);
            }
            final SqlTableId tableName;
            final List<SqlColumnId> columnNames;
            if (columns.isEmpty()) {
                if (isUpdate(sqlCommand)) {
                    Map.Entry<SqlTableId, Set<SqlColumnId>> updatedDataFields = ColumnTableAnalyzer.getUpdatedDataFields(sqlCommand);
                    tableName = updatedDataFields.getKey();
                    columnNames = Collections.singletonList(new SqlColumnId("*"));
                } else if (isDelete(sqlCommand)) {
                    tableName = ColumnTableAnalyzer.getDeletedTable(sqlCommand);
                    columnNames = Collections.singletonList(new SqlColumnId("*"));
                } else {
                    throw new IllegalStateException("SQL command should only be SELECT, UPDATE or DELETE");
                }
            } else {
                Map.Entry<SqlTableId, Set<SqlColumnId>> tableToColumns = columns.entrySet().iterator().next();
                tableName = tableToColumns.getKey();
                columnNames = tableToColumns.getValue().stream().sorted().collect(Collectors.toList());
            }
            select = SqlSelectBuilder.buildSelect(schema.databaseType, tableName, columnNames);
        }

        QueryResult data;
        try {
            data = SqlScriptRunner.execCommand(connection, select);
        } catch (SQLException e) {
            SimpleLogger.uniqueWarn("Failed to execute query for retrieving data for computing SQL heuristics: " + select);
            return new SqlDistanceWithMetrics(Double.MAX_VALUE, 0, true);
        }

        return HeuristicsCalculator.computeDistance(sqlCommand, schema, taintHandler, data);
    }

    /**
     * Check the fields involved in the WHERE clause (if any).
     * Return a map from table name to column names of the involved fields.
     */
    Map<SqlTableId, Set<SqlColumnId>> extractColumnsInvolvedInWhere(Statement statement) {

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

                String fullyQualifiedTableName = context.getFullyQualifiedTableName(column);
//                String tableName = context.getTableName(column);

                if (fullyQualifiedTableName.equalsIgnoreCase(SqlNameContext.UNNAMED_TABLE)) {
                    // TODO handle it properly when ll have support for sub-selects
                    return;
                }

                String columnName = column.getColumnName().toLowerCase();

                if (!context.hasColumn(fullyQualifiedTableName, columnName)) {

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

                    if (BooleanLiteralsHelper.isBooleanLiteral(columnName)) {
                        //case in which a boolean constant is wrongly treated as a column name.
                        //TODO not sure what we can really do here without modifying the parser
                    } else {
                        SimpleLogger.warn("Cannot find column '" + columnName + "' in table '" + fullyQualifiedTableName + "'");
                    }
                    return;
                }
                final SqlTableId tableId = SqlTableIdParser.parseFullyQualifiedTableName(fullyQualifiedTableName, schema.databaseType);
                result.putIfAbsent(tableId, new HashSet<>());
                Set<SqlColumnId> columnIds = result.get(tableId);
                columnIds.add(new SqlColumnId(columnName));
            }
        };

        where.accept(visitor);

        return result;
    }

    static void mergeNewData(
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

    public boolean isCompleteSqlHeuristics() {
        return completeSqlHeuristics;
    }

    public void setCompleteSqlHeuristics(boolean completeSqlHeuristics) {
        this.completeSqlHeuristics = completeSqlHeuristics;
    }
}
