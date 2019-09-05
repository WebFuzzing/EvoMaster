package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.controller.db.QueryResult;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.internal.db.ParserUtils.*;

/**
 * Class used to act upon SQL commands executed by the SUT
 */
public class SqlHandler {

    /**
     * Computing heuristics on SQL is expensive, as we need to run
     * further queries. So, we buffer them, and execute them only
     * if needed (ie, lazy initialization)
     */
    private final List<String> buffer;

    /**
     * The heuristics based on the SQL execution
     */
    private final List<PairCommandDistance> distances;

    //see ExecutionDto
    private final Map<String, Set<String>> queriedData;
    private final Map<String, Set<String>> updatedData;
    private final Map<String, Set<String>> insertedData;
    private final Map<String, Set<String>> failedWhere;
    private final List<String> deletedData;

    private int numberOfSqlCommands;

    private volatile Connection connection;

    private volatile boolean calculateHeuristics;

    private volatile boolean extractSqlExecution;

    public SqlHandler() {
        buffer = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();
        queriedData = new ConcurrentHashMap<>();
        updatedData = new ConcurrentHashMap<>();
        insertedData = new ConcurrentHashMap<>();
        failedWhere = new ConcurrentHashMap<>();
        deletedData = new CopyOnWriteArrayList<>();

        calculateHeuristics = true;
        numberOfSqlCommands = 0;
    }

    public void reset() {
        buffer.clear();
        distances.clear();
        queriedData.clear();
        updatedData.clear();
        insertedData.clear();
        failedWhere.clear();
        deletedData.clear();
        numberOfSqlCommands = 0;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void handle(String sql) {
        Objects.requireNonNull(sql);

        if(!calculateHeuristics && !extractSqlExecution){
            return;
        }

        buffer.add(sql);

        if (isSelect(sql)) {
            mergeNewData(queriedData, ColumnTableAnalyzer.getSelectReadDataFields(sql));
        } else if(isDelete(sql)){
            deletedData.addAll(ColumnTableAnalyzer.getDeletedTables(sql));
        } else if(isInsert(sql)){
            mergeNewData(insertedData, ColumnTableAnalyzer.getInsertedDataFields(sql));
        } else if(isUpdate(sql)){
            mergeNewData(updatedData, ColumnTableAnalyzer.getUpdatedDataFields(sql));
        }

        numberOfSqlCommands++;
    }

    public ExecutionDto getExecutionDto() {

        if(!calculateHeuristics && !extractSqlExecution){
            return null;
        }

        ExecutionDto executionDto = new ExecutionDto();
        executionDto.queriedData.putAll(queriedData);
        executionDto.failedWhere.putAll(failedWhere);
        executionDto.insertedData.putAll(insertedData);
        executionDto.updatedData.putAll(updatedData);
        executionDto.deletedData.addAll(deletedData);
        executionDto.numberOfSqlCommands = this.numberOfSqlCommands;

        return executionDto;
    }

    public List<PairCommandDistance> getDistances() {

        if (connection == null || !calculateHeuristics) {
            return distances;
        }


        buffer.stream()
                .forEach(sql -> {
                    /*
                        Note: even if the Connection we got to analyze
                        the DB is using P6Spy, that would not be a problem,
                        as output SQL would not end up on the buffer instance
                        we are iterating on (copy on write), and we clear
                        the buffer after this loop.
                     */
                    if (isSelect(sql) || isDelete(sql) || isUpdate(sql)) {
                        double dist = computeDistance(sql);
                        distances.add(new PairCommandDistance(sql, dist));
                    }
                });
        //side effects on buffer is not important, as it is just a cache
        buffer.clear();

        return distances;
    }


    private Double computeDistance(String command) {

        if (connection == null) {
            throw new IllegalStateException("Trying to calculate SQL distance with no DB connection");
        }

        Statement statement;

        try {
            statement = CCJSqlParserUtil.parse(command);
        } catch (Exception e) {
            SimpleLogger.uniqueWarn("Cannot handle command: " + command + "\n" + e.toString());
            return Double.MAX_VALUE;
        }


        Map<String, Set<String>> columns = extractColumnsInvolvedInWhere(statement);

        /*
            even if columns.isEmpty(), we need to check if any data was present
         */

        double dist;
        if(columns.isEmpty()){
            //TODO check if table(s) not empty, and give >0 otherwise
            dist = 0;
        } else {
            dist = getDistanceForWhere(command, columns);
        }

        if (dist > 0) {
            mergeNewData(failedWhere, columns);
        }

        return dist;
    }

    private double getDistanceForWhere(String command, Map<String, Set<String>> columns) {
        String select;

        /*
           TODO:
           this might be likely unnecessary... we are only interested in the variables used
           in the WHERE. Furthermore, this would not support DELETE/INSERT/UPDATE.
           So, we just need to create a new SELECT based on that.
           But SELECT could be complex with many JOINs... whereas DIP would be simple(r)?

           TODO: we need a general solution
         */
        if(isSelect(command)) {
            select = SelectTransformer.addFieldsToSelect(command);
            select = SelectTransformer.removeConstraints(select);
            select = SelectTransformer.removeOperations(select);
        } else {
            if(columns.size() > 1){
                SimpleLogger.uniqueWarn("Cannot analyze: " + command);
            }
            Map.Entry<String, Set<String>> mapping = columns.entrySet().iterator().next();
            select = createSelectForSingleTable(mapping.getKey(), mapping.getValue());
        }

        QueryResult data;

        try {
            data = SqlScriptRunner.execCommand(connection, select);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return HeuristicsCalculator.computeDistance(command, data);
    }

    private String createSelectForSingleTable(String tableName, Set<String> columns){

        StringBuilder buffer = new StringBuilder();
        buffer.append("SELECT ");

        String variables = columns.stream().collect(Collectors.joining(", "));

        buffer.append(variables);
        buffer.append(" FROM ");
        buffer.append(tableName);

        return buffer.toString();
    }

    /**
     *  Check the fields involved in the WHERE clause (if any).
     *  Return a map from table name to column names of the involved fields.
     */
    private static Map<String, Set<String>> extractColumnsInvolvedInWhere(Statement statement) {

        /*
           TODO
           following does not handle the case of sub-selects involving other
           tables... but likely that is not something we need to support right now
         */

        Map<String, Set<String>> data = new HashMap<>();

        SqlNameContext context = new SqlNameContext(statement);

        Expression where = ParserUtils.getWhere(statement);
        if (where == null) {
            return data;
        }

        ExpressionVisitor visitor = new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column column) {
                String cn = column.getColumnName();
                String tn = context.getTableName(column);

                if(tn.equalsIgnoreCase(SqlNameContext.UNNAMED_TABLE)){
                    // TODO handle it properly when ll have support for sub-selects
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
}
