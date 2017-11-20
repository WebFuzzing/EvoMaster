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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class SqlHandler {

    private final List<String> buffer;
    private final List<Double> distances;

    private volatile Connection connection;

    public SqlHandler() {
        buffer = new CopyOnWriteArrayList<>();
        distances = new ArrayList<>();
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void handle(String sql) {
        Objects.requireNonNull(sql);

        buffer.add(sql);
    }


    public void reset() {
        buffer.clear();
        distances.clear();
    }

    public List<Double> getDistances() {

        if(connection == null){
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
        buffer.clear();

        return distances;
    }

    private boolean isSelect(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }


    public static boolean isValidSql(String sql){

        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (JSQLParserException e) {
            return false;
        }
    }

    public Double computeDistance(String select) {

        if(connection == null){
            throw new IllegalStateException("Trying to calculate SQL distance with no DB connection");
        }

        try {
            Select stmt = (Select) CCJSqlParserUtil.parse(select);
        } catch (Exception | TokenMgrError e) {
            SimpleLogger.uniqueWarn("Cannot handle select query: " + select +"\n" + e.toString());
            return Double.MAX_VALUE;
        }

        select = SelectHeuristics.addFieldsToSelect(select);
        select = SelectHeuristics.removeConstraints(select);
        select = SelectHeuristics.removeOperations(select);

        QueryResult data;

        try {
             data = SqlScriptRunner.execCommand(connection, select);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        double dist = SelectHeuristics.computeDistance(select, data);

        return dist;
    }
}
