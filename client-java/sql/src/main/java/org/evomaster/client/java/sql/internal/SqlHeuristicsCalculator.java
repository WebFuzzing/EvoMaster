package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;

import java.util.Collection;
import java.util.List;

import static org.evomaster.client.java.sql.internal.SqlParserUtils.*;

public class SqlHeuristicsCalculator {

    public static double C = 0.1;
    public static double C_BETTER = C + (C / 2);
    public static Truthness TRUE_TRUTHNESS = new Truthness(1, C);
    public static Truthness FALSE_TRUTHNESS = TRUE_TRUTHNESS.invert();
    public static Truthness FALSE_TRUTHNESS_BETTER = new Truthness(C_BETTER, 1);

    private final QueryResultSet queryResultSet;
    private final SqlNameContext sqlNameContext;
    private SqlHeuristicsCalculator(SqlNameContext sqlNameContext, QueryResult[] data) {
        final boolean isCaseSensitive = false;
        this.sqlNameContext = sqlNameContext;
        this.queryResultSet = new QueryResultSet(isCaseSensitive);
        for (QueryResult queryResult : data) {
            queryResultSet.addQueryResult(queryResult);
        }
    }

    public static SqlDistanceWithMetrics computeDistance(String sqlCommand,
                                                         DbInfoDto schema,
                                                         TaintHandler taintHandler,
                                                         QueryResult... data) {

        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        SqlNameContext sqlNameContext = new SqlNameContext(parsedSqlCommand);
        if (schema != null) {
            sqlNameContext.setSchema(schema);
        }
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(sqlNameContext, data);
        Truthness t = calculator.computeCommand(parsedSqlCommand);
        double distanceToTrue = 1 - t.getOfTrue();
        return new SqlDistanceWithMetrics(distanceToTrue, 0, false);
    }

    private Truthness computeCommand(Statement parsedSqlCommand) {
        final Expression whereClause = getWhere(parsedSqlCommand);
        final FromItem fromItem = getFrom(parsedSqlCommand);
        final List<Join> joins = getJoins(parsedSqlCommand);
        if (fromItem == null && joins == null) {
            /**
             * Result will depend on the contents of the virtual table
             */
            return getTruthnessForTable(null);
        } else if (fromItem != null && joins == null && whereClause == null) {
            return getTruthnessForTable(fromItem);
        } else if (fromItem != null && joins == null && whereClause != null) {
            return getTruthnessForCondition(whereClause, fromItem);
        } else if (fromItem != null && joins != null && whereClause == null) {
            final Join join = joins.get(0);
            final FromItem leftFromItem = fromItem;
            final FromItem rightFromItem = join.getRightItem();
            final Collection<Expression> onExpressions = join.getOnExpressions();
            if (join.isLeft()) {
                return getTruthnessForTable(leftFromItem);
            } else if (join.isRight()) {
                return getTruthnessForTable(rightFromItem);
            } else if (join.isCross()) {
                Truthness truthnessLeftTable = getTruthnessForTable(leftFromItem);
                Truthness truthnessRightTable = getTruthnessForTable(rightFromItem);
                return TruthnessUtils.buildAndAggregationTruthness(truthnessLeftTable, truthnessRightTable);
            } else {
                // inner join?
            }

        }

        return null;
    }

    private Truthness getTruthnessForCondition(Expression whereClause, FromItem fromItem) {

        double maxOfTrue = 0.0d;
        int rowCount = 0;
        QueryResult queryResult = getQueryResultForFromItem(fromItem);
        if (queryResult.isEmpty()) {
            return FALSE_TRUTHNESS;
        } else {
            for (DataRow row : queryResult.seeRows()) {
                Truthness t = getTruthnessForExpression(whereClause, row);
                if (t.isTrue()) {
                    return TRUE_TRUTHNESS;
                } else {
                    if (t.getOfTrue() > maxOfTrue) {
                        maxOfTrue = t.getOfTrue();
                    }
                }
                rowCount++;
            }
            return TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
        }


    }

    private Truthness getTruthnessForExpression(Expression whereClause, DataRow row) {
        SqlExpressionEvaluator expressionEvaluator = new SqlExpressionEvaluator(sqlNameContext, row);
        whereClause.accept(expressionEvaluator);
        return expressionEvaluator.getEvaluatedTruthness();
    }

    private Truthness getTruthnessForTable(FromItem fromItem) {
        final QueryResult tableData = getQueryResultForFromItem(fromItem);
        final int len = tableData.size();
        final Truthness t = TruthnessUtils.getTruthnessToEmpty(len).invert();
        return t;
    }

    private QueryResult getQueryResultForFromItem(FromItem fromItem) {
        final QueryResult tableData;
        if (fromItem == null) {
            tableData = queryResultSet.getQueryResultForVirtualTable();
        } else {
            if (!SqlParserUtils.isTable(fromItem)) {
                throw new IllegalArgumentException("Cannot compute Truthness for form item that it is not a table " + fromItem);
            }
            String tableName = SqlParserUtils.getTableName(fromItem);
            tableData = queryResultSet.getQueryResultForNamedTable(tableName);
        }
        return tableData;
    }


}
