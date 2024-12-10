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
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.utils.SimpleLogger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final Map<String, Set<String>> queriedData;
    private final Map<String, Set<String>> updatedData;
    private final Map<String, Set<String>> insertedData;
    private final Map<String, Set<String>> failedWhere;
    private final List<String> deletedData;
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
            deletedData.addAll(ColumnTableAnalyzer.getDeletedTables(sqlCommand));
        } else if (isInsert(sqlCommand)) {
            mergeNewData(insertedData, ColumnTableAnalyzer.getInsertedDataFields(sqlCommand));
        } else if (isUpdate(sqlCommand)) {
            mergeNewData(updatedData, ColumnTableAnalyzer.getUpdatedDataFields(sqlCommand));
        }

    }

    public SqlExecutionsDto getExecutionDto() {

        if (!calculateHeuristics && !extractSqlExecution) {
            return null;
        }

        SqlExecutionsDto sqlExecutionsDto = new SqlExecutionsDto();
        sqlExecutionsDto.queriedData.putAll(queriedData);
        sqlExecutionsDto.failedWhere.putAll(failedWhere);
        sqlExecutionsDto.insertedData.putAll(insertedData);
        sqlExecutionsDto.updatedData.putAll(updatedData);
        sqlExecutionsDto.deletedData.addAll(deletedData);
        sqlExecutionsDto.numberOfSqlCommands = this.numberOfSqlCommands;
        sqlExecutionsDto.sqlParseFailureCount = this.sqlParseFailureCount;
        sqlExecutionsDto.sqlExecutionLogDtoList.addAll(executedSqlCommands);
        return sqlExecutionsDto;
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

                if (sqlExecutionLogDto.threwSqlExeception!=true
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

        Map<String, Set<String>> columns = extractColumnsInvolvedInWhere(parsedStatement);

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


    private SqlDistanceWithMetrics getDistanceForWhereBasedOnInsertion(String sqlCommand, Map<String, Set<String>> columns, List<InsertionDto> insertionDtos) {
        QueryResult[] data = QueryResultTransformer.convertInsertionDtosToQueryResults(insertionDtos, columns, schema);
        assert data != null;
        return HeuristicsCalculator.computeDistance(sqlCommand, schema, taintHandler, advancedHeuristics, data);
    }


    private SqlDistanceWithMetrics getDistanceForWhere(String sqlCommand, Map<String, Set<String>> columns) {
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
            final String tableName;
            final Set<String> columnNames;
            if (columns.isEmpty()) {
                if (isUpdate(sqlCommand)) {
                    Map<String, Set<String>> mapping = ColumnTableAnalyzer.getUpdatedDataFields(sqlCommand);
                    if (mapping.size() != 1) {
                        //TODO need to handle special cases of multi-tables with JOINs
                        throw new IllegalArgumentException("Cannot handle delete: " + sqlCommand);
                    } else {
                        tableName= mapping.entrySet().iterator().next().getKey();
                        columnNames = Collections.singleton("*");
                    }
                } else if (isDelete(sqlCommand)) {
                    Set<String> deletedTables = ColumnTableAnalyzer.getDeletedTables(sqlCommand);
                    if (deletedTables.size()!=1) {
                        //TODO need to handle special cases of multi-tables with JOINs
                        throw new IllegalArgumentException("Cannot handle delete: " + sqlCommand);
                    } else {
                        tableName = deletedTables.iterator().next();
                        columnNames = Collections.singleton("*");
                    }
                } else {
                    throw new IllegalStateException("SQL command should only be SELECT, UPDATE or DELETE");
                }
            } else{
                Map.Entry<String, Set<String>> tableToColumns = columns.entrySet().iterator().next();
                tableName = tableToColumns.getKey();
                columnNames = tableToColumns.getValue();
            }
            select = createSelectForSingleTable(tableName, columnNames);
        }

        QueryResult data;
        try {
            data = SqlScriptRunner.execCommand(connection, select);
        } catch (SQLException e) {
            SimpleLogger.uniqueWarn("Failed to execute query for retrieving data for computing SQL heuristics: " + select);
            return new SqlDistanceWithMetrics(Double.MAX_VALUE, 0, true);
        }

        return HeuristicsCalculator.computeDistance(sqlCommand, schema, taintHandler, advancedHeuristics, data);
    }

    private String createSelectForSingleTable(String tableName, Set<String> columns) {

        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ");

        String variables = String.join(", ", columns);

        buffer.append(variables);
        buffer.append(" FROM ");
        buffer.append(tableName);

        return buffer.toString();
    }

    /**
     * Check the fields involved in the WHERE clause (if any).
     * Return a map from table name to column names of the involved fields.
     */
    public Map<String, Set<String>> extractColumnsInvolvedInWhere(Statement statement) {

        /*
           TODO
           following does not handle the case of sub-selects involving other
           tables... but likely that is not something we need to support right now
         */

        Map<String, Set<String>> data = new HashMap<>();

        // move getWhere before SqlNameContext, otherwise null where would cause exception in new SqlNameContext
        Expression where = SqlParserUtils.getWhere(statement);
        if (where == null) {
            return data;
        }

        SqlNameContext context = new SqlNameContext(statement);
        if (schema != null) {
            context.setSchema(schema);
        }

        ExpressionVisitor visitor = new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column column) {

                String tn = context.getTableName(column);

                if (tn.equalsIgnoreCase(SqlNameContext.UNNAMED_TABLE)) {
                    // TODO handle it properly when ll have support for sub-selects
                    return;
                }

                String cn = column.getColumnName().toLowerCase();

                if (!context.hasColumn(tn, cn)) {

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

                    if (booleanConstantNames.contains(cn)) {
                        //case in which a boolean constant is wrongly treated as a column name.
                        //TODO not sure what we can really do here without modifying the parser
                    } else {
                        SimpleLogger.warn("Cannot find column '" + cn + "' in table '" + tn + "'");
                    }
                    return;
                }

                data.putIfAbsent(tn, new HashSet<>());
                Set<String> set = data.get(tn);
                set.add(cn);
            }
        };

        where.accept(visitor);

        return data;
    }

    private static void mergeNewData(
            Map<String, Set<String>> current,
            Map<String, Set<String>> toAdd
    ) {

        for (Map.Entry<String, Set<String>> e : toAdd.entrySet()) {
            String key = e.getKey();
            Set<String> values = e.getValue();

            Set<String> existing = current.get(key);

            if (existing != null && existing.contains("*")) {
                //nothing to do
                continue;
            }

            if (existing == null) {
                existing = new HashSet<>(values);
                current.put(key, existing);
            } else {
                existing.addAll(values);
            }

            if (existing.size() > 1 && existing.contains("*")) {
                /*
                    remove unnecessary columns, as anyway we take
                    everything with *
                 */
                existing.clear();
                existing.add("*");
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
