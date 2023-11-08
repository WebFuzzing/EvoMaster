package org.evomaster.client.java.sql.distance.standard;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import static org.evomaster.client.java.sql.internal.ParserUtils.isSelect;

/**
 * Class used to calculate SQL distance
 */
public class StandardDistance {

    private volatile Connection connection;
    private volatile DbSchemaDto schema;
    private final TaintHandler taintHandler;

    public StandardDistance(Connection connection, DbSchemaDto schema, TaintHandler taintHandler) {
        this.connection = connection;
        this.schema = schema;
        this.taintHandler = taintHandler;
    }

    public double calculateDistance(String command, Map<String, Set<String>> columns) {
        String select;

        /*
           TODO:
           this might be likely unnecessary... we are only interested in the variables used
           in the WHERE. Furthermore, this would not support DELETE/INSERT/UPDATE.
           So, we just need to create a new SELECT based on that.
           But SELECT could be complex with many JOINs... whereas DIP would be simple(r)?

           TODO: we need a general solution
         */
        if (isSelect(command)) {
            select = SelectTransformer.addFieldsToSelect(command);
            select = SelectTransformer.removeConstraints(select);
            select = SelectTransformer.removeOperations(select);
        } else {
            if (columns.size() > 1) {
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

        return HeuristicsCalculator.computeDistance(command, data, schema, taintHandler);
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
}
