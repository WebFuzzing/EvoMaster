package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.QueryResult;

import static org.evomaster.client.java.sql.internal.SqlParserUtils.getFrom;
import static org.evomaster.client.java.sql.internal.SqlParserUtils.getWhere;

public class SqlHeuristicsCalculator {

    public static SqlDistanceWithMetrics computeDistance(String sqlCommand,
                                                         DbInfoDto schema,
                                                         TaintHandler taintHandler,
                                                         QueryResult... data) {

        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Expression whereClause= getWhere(parsedSqlCommand);
        FromItem fromItem = getFrom(parsedSqlCommand);

        if (fromItem == null && whereClause == null) {
            return new SqlDistanceWithMetrics(0.0,0,false);
        }


        return null;
    }
}
