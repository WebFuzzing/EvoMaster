package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrError;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.db.QueryResult;
import org.evomaster.clientJava.controller.db.SqlScriptRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /**
     * Keep track of which tables/columns have been read
     */
    private final Map<String, Set<String>> readData;

    /**
     * SQL Select commands that did not return any data
     */
    private final Set<String> emptySqlSelects;


    private volatile Connection connection;


    public SqlHandler() {
        buffer = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();
        readData = new ConcurrentHashMap<>();
        emptySqlSelects = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public void reset() {
        buffer.clear();
        distances.clear();
        readData.clear();
        emptySqlSelects.clear();
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void handle(String sql) {
        Objects.requireNonNull(sql);

        buffer.add(sql);

        handleReadData(sql);
    }

    private void handleReadData(String sql) {

        if(! isSelect(sql)){
            return;
        }

        Map<String, Set<String>> current = SelectHeuristics.getReadDataFields(sql);


        for(Map.Entry<String, Set<String>> e : current.entrySet()){
            String key = e.getKey();
            Set<String> values = e.getValue();

            Set<String> existing = readData.get(key);

            if(existing != null && existing.contains("*")){
                //nothing to do
                continue;
            }

            if(existing == null){
                existing = new HashSet<>(values);
                readData.put(key, existing);
            } else {
                existing.addAll(values);
            }

            if(existing.size() > 1 && existing.contains("*")){
                /*
                    remove unnecessary columns, as anyway we take
                    everything with *
                 */
                existing.clear();
                existing.add("*");
            }
        }
    }


    /**
     * key -> table name
     * <br>
     * value -> column names. "*" means all columns
     * @return a map of which data (tables and columns) have
     * been read from SQL database
     */
    public Map<String, Set<String>> getReadData() {
        return readData;
    }


    public Set<String> getEmptySqlSelects() {
        return emptySqlSelects;
    }

    public List<Double> getDistances() {

        if (connection == null) {
            return distances;
        }

        buffer.stream()
                .filter(sql -> isSelect(sql))
                .forEach(sql -> {
                    /*
                        Note: even if the Connection we got to analyze
                        the DB is using P6Spy, that would not be a problem,
                        as output SQL would not end up on the buffer instance
                        we are iterating on (copy on write), and we clear
                        the buffer after this loop.
                     */
                    double dist = computeDistance(sql);
                    distances.add(dist);
                });
        //side effects on buffer is not important, as it is just a cache
        buffer.clear();

        return distances;
    }

    private boolean isSelect(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }


    public static boolean isValidSql(String sql) {

        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (JSQLParserException e) {
            return false;
        }
    }

    public Double computeDistance(String select) {

        if (connection == null) {
            throw new IllegalStateException("Trying to calculate SQL distance with no DB connection");
        }

        try {
            CCJSqlParserUtil.parse(select);
        } catch (Exception | TokenMgrError e) {
            SimpleLogger.uniqueWarn("Cannot handle select query: " + select + "\n" + e.toString());
            return Double.MAX_VALUE;
        }

        String modified = SelectHeuristics.addFieldsToSelect(select);
        modified = SelectHeuristics.removeConstraints(modified);
        modified = SelectHeuristics.removeOperations(modified);

        QueryResult data;

        try {
            data = SqlScriptRunner.execCommand(connection, modified);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        double dist = SelectHeuristics.computeDistance(select, data);

        if(dist > 0){
            emptySqlSelects.add(select);
        }

        return dist;
    }
}
