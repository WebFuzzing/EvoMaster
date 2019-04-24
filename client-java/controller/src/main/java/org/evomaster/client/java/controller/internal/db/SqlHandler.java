package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.client.java.controller.db.QueryResult;
import org.evomaster.client.java.controller.db.SqlScriptRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.evomaster.client.java.controller.internal.db.ParserUtils.isSelect;

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
    private final List<Double> distances;

    //see ExecutionDto
    private final Map<String, Set<String>> queriedData;
    private final Map<String, Set<String>> updatedData;
    private final Map<String, Set<String>> insertedData;
    private final Map<String, Set<String>> failedWhere;
    private final List<String> deletedData;


    private volatile Connection connection;


    public SqlHandler() {
        buffer = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();
        queriedData = new ConcurrentHashMap<>();
        updatedData = new ConcurrentHashMap<>();
        insertedData = new ConcurrentHashMap<>();
        failedWhere = new ConcurrentHashMap<>();
        deletedData = new CopyOnWriteArrayList<>();
    }

    public void reset() {
        buffer.clear();
        distances.clear();
        queriedData.clear();
        updatedData.clear();
        insertedData.clear();
        failedWhere.clear();
        deletedData.clear();
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void handle(String sql) {
        Objects.requireNonNull(sql);

        buffer.add(sql);

        //TODO Delete/Insert/Update
        if (isSelect(sql)) {
            mergeNewData(queriedData, ColumnTableAnalyzer.getSelectReadDataFields(sql));
        }
    }

    public ExecutionDto getExecutionDto() {
        ExecutionDto executionDto = new ExecutionDto();
        executionDto.queriedData.putAll(queriedData);
        executionDto.failedWhere.putAll(failedWhere);
        executionDto.insertedData.putAll(insertedData);
        executionDto.updatedData.putAll(updatedData);
        executionDto.deletedData.addAll(deletedData);

        return executionDto;
    }

    public List<Double> getDistances() {

        if (connection == null) {
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
                    if (isSelect(sql)) { //TODO Delete/Insert/Update
                        double dist = computeDistance(sql);
                        distances.add(dist);
                    }
                });
        //side effects on buffer is not important, as it is just a cache
        buffer.clear();

        return distances;
    }


    public Double computeDistance(String command) {

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

        /*
           TODO
           following does not handle the case of sub-selects involving other
           tables... but likely that is not something we need to support right now

           TODO:
           this might be likely unnecessary... we are only interested in the variables used
           in the WHERE. Furthermore, this would not support DELETE/INSERT/UPDATE.
           So, we just need to create a new SELECT based on that.
           But SELECT could be complex with many JOINs... whereas DIP would be simple(r)?
         */
        String modified = SelectHeuristics.addFieldsToSelect(command);
        modified = SelectHeuristics.removeConstraints(modified);
        modified = SelectHeuristics.removeOperations(modified);

        QueryResult data;

        try {
            data = SqlScriptRunner.execCommand(connection, modified);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        double dist = SelectHeuristics.computeDistance(command, data);

        if (dist > 0) {
            mergeNewData(failedWhere, extractColumnsInvolvedInWhere(statement));
        }

        return dist;
    }

    private static Map<String, Set<String>> extractColumnsInvolvedInWhere(Statement statement) {

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

}
