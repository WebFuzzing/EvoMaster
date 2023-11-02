package org.evomaster.client.java.controller.internal.db.sql;

import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.StatementClassReplacement;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EMSqlScriptRunner {

    public static QueryResult execCommand(Connection conn, String command, boolean instrumented) throws SQLException {

        if(!instrumented){
            return SqlScriptRunner.execCommand(conn,command);
        }

        Statement statement = conn.createStatement();

        SimpleLogger.debug("Executing DB command:");
        SimpleLogger.debug(command);

        try {
                /*
                    this is needed only in the tests for EM itself... note that we cannot
                    instrument classes in the org.evomaster package
                 */
            StatementClassReplacement.execute(statement, command);
        } catch (SQLException e) {
            statement.close();
            String errText = String.format("Error executing '%s': %s", command, e.getMessage());
            throw new SQLException(errText, e);
        }

        ResultSet result = statement.getResultSet();
        QueryResult queryResult = new QueryResult(result);

        statement.close();

        return queryResult;
    }
}
