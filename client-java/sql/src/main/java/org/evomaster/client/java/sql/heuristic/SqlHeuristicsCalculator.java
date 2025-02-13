package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlNameContext;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.sql.internal.SqlParserUtils.*;

public class SqlHeuristicsCalculator {

    public static double C = 0.1;
    public static double C_BETTER = C + (C / 2);
    public static Truthness TRUE_TRUTHNESS = new Truthness(1, C);
    public static Truthness FALSE_TRUTHNESS = TRUE_TRUTHNESS.invert();
    public static Truthness FALSE_TRUTHNESS_BETTER = new Truthness(C_BETTER, 1);

    private final QueryResultSet queryResultSet;
    private final SqlNameContext sqlNameContext;
    private final TaintHandler taintHandler;

    SqlHeuristicsCalculator(SqlNameContext sqlNameContext, TaintHandler taintHandler, QueryResult[] data) {
        final boolean isCaseSensitive = false;
        this.sqlNameContext = sqlNameContext;
        this.queryResultSet = new QueryResultSet(isCaseSensitive);
        this.taintHandler = taintHandler;
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
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(sqlNameContext, taintHandler, data);
        Truthness t = calculator.calculateHeuristicQuery(parsedSqlCommand).getTruthness();
        double distanceToTrue = 1 - t.getOfTrue();
        return new SqlDistanceWithMetrics(distanceToTrue, 0, false);
    }

    private SqlHeuristicResult calculateHeuristicRowSet(FromItem fromItem) {
        return calculateHeuristicRowSet(fromItem, null);
    }

    private SqlHeuristicResult calculateHeuristicRowSet(FromItem fromItem, List<Join> joins) {

        if (fromItem == null) {
            /**
             * Handle the case where no FROM is used (e.g. SELECT 42;)
             */
            return new SqlHeuristicResult(TRUE_TRUTHNESS, queryResultSet.getQueryResultForVirtualTable());
        }

        if (SqlParserUtils.isTable(fromItem) && (joins == null || joins.isEmpty())) {
            /**
             * Handles the case where the FROM is a table
             */
            return calculateHeuristicFromItem(fromItem);
        }

        if (SqlParserUtils.isSubquery(fromItem) && (joins == null || joins.isEmpty())) {
            /**
             * Handles the case when FROM is a subquery
             */
            final Statement subquery = SqlParserUtils.getSubquery(fromItem);
            return calculateHeuristicQuery(subquery);
        }

        if (joins != null && !joins.isEmpty()) {
            SqlHeuristicResult joinResult = calculateHeuristicRowSet(fromItem);
            for (Join join: joins) {
                if (join.isInnerJoin()) {
                    joinResult = calculateHeuristicInnerJoin(joinResult, join);
                } else if (join.isLeft()) {
                    joinResult = calculateHeuristicLeftJoin(joinResult, join);
                } else if (join.isRight()) {
                    joinResult = calculateHeuristicRightJoin(joinResult, join);
                } else if (join.isCross()) {
                    joinResult = calculateHeuristicCrossJoin(joinResult, join);
                }
            }
            return joinResult;
        }

        throw new IllegalArgumentException();
    }

    private SqlHeuristicResult calculateHeuristicCrossJoin(SqlHeuristicResult leftRowSetResult, Join crossJoin) {
        if (!crossJoin.isCross()) {
            throw new IllegalArgumentException("Join is not a CROSS JOIN");
        }

        final FromItem rightFromItem = crossJoin.getRightItem();
        SqlHeuristicResult rightRowSetResult = calculateHeuristicRowSet(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();
        final QueryResult cartesianProduct = QueryResultUtils.createCartesianProduct(leftQueryResult, rightQueryResult);

        Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(leftRowSetResult.getTruthness(),
                rightRowSetResult.getTruthness());

        return new SqlHeuristicResult(truthness, cartesianProduct);
    }


    private SqlHeuristicResult calculateHeuristicInnerJoin(SqlHeuristicResult leftRowSetResult, Join innerJoin) {
        if (!innerJoin.isInnerJoin()) {
            throw new IllegalArgumentException("Join is not a INNER JOIN");
        }

        final FromItem rightFromItem = innerJoin.getRightItem();
        final Collection<Expression> onExpressions = innerJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = calculateHeuristicRowSet(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();
        final QueryResult cartesianProduct = QueryResultUtils.createCartesianProduct(leftQueryResult, rightQueryResult);

        SqlHeuristicResult conditionResult = calculateHeuristicCondition(onExpressions, cartesianProduct);

        Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(leftRowSetResult.getTruthness(),
                rightRowSetResult.getTruthness(),
                conditionResult.getTruthness());
        return new SqlHeuristicResult(truthness, conditionResult.getQueryResult());
    }

    private SqlHeuristicResult calculateHeuristicFromItem(FromItem fromItem) {
        Objects.requireNonNull(fromItem);

        final QueryResult tableContents = getQueryResultForFromItem(fromItem);
        final int len = tableContents.size();
        final Truthness truthness = TruthnessUtils.getTruthnessToEmpty(len).invert();
        return new SqlHeuristicResult(truthness, tableContents);
    }

    private SqlHeuristicResult calculateHeuristicRightJoin(SqlHeuristicResult leftRowSetResult, Join rightJoin) {
        if (!rightJoin.isRight()) {
            throw new IllegalArgumentException("Join is not a RIGHT JOIN");
        }

        final FromItem rightFromItem = rightJoin.getRightItem();
        final Collection<Expression> onExpressions = rightJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = calculateHeuristicRowSet(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();

        final QueryResult queryResult = createRightJoin(leftQueryResult, rightQueryResult, onExpressions);

        return new SqlHeuristicResult(rightRowSetResult.getTruthness(), queryResult);
    }

    private QueryResult createRightJoin(QueryResult leftQueryResult, QueryResult rightQueryResult, Collection<Expression> onExpressions) {
        final QueryResult queryResult = QueryResultUtils.createEmptyCartesianProduct(leftQueryResult, rightQueryResult);
        for (DataRow rightRow : rightQueryResult.seeRows()) {
            boolean foundMatch = false;
            for (DataRow leftRow : leftQueryResult.seeRows()) {
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, rightRow, queryResult.seeVariableDescriptors());
                final Truthness truthness = evaluateAllConditions(onExpressions, joinedDataRow);
                if (truthness.isTrue()) {
                    queryResult.addRow(joinedDataRow);
                    foundMatch = true;
                }
            }
            if (!foundMatch) {
                final DataRow nullDataRow = QueryResultUtils.createDataRowOfNullValues(leftQueryResult);
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(nullDataRow, rightRow, queryResult.seeVariableDescriptors());
                queryResult.addRow(joinedDataRow);
            }
        }
        return queryResult;
    }

    private SqlHeuristicResult calculateHeuristicLeftJoin(SqlHeuristicResult leftRowSetResult, Join leftJoin) {
        if (!leftJoin.isLeft()) {
            throw new IllegalArgumentException("Join is not a LEFT JOIN");
        }

        final FromItem rightFromItem = leftJoin.getRightItem();
        final Collection<Expression> onExpressions = leftJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = calculateHeuristicRowSet(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();

        final QueryResult queryResult = createLeftJoin(leftQueryResult, rightQueryResult, onExpressions);

        return new SqlHeuristicResult(leftRowSetResult.getTruthness(), queryResult);
    }

    private QueryResult createLeftJoin(QueryResult leftQueryResult, QueryResult rightQueryResult, Collection<Expression> onExpressions) {
        final QueryResult queryResult = QueryResultUtils.createEmptyCartesianProduct(leftQueryResult, rightQueryResult);
        for (DataRow leftRow : leftQueryResult.seeRows()) {
            boolean foundMatch = false;
            for (DataRow rightRow : rightQueryResult.seeRows()) {
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, rightRow, queryResult.seeVariableDescriptors());
                final Truthness truthness = evaluateAllConditions(onExpressions, joinedDataRow);
                if (truthness.isTrue()) {
                    queryResult.addRow(joinedDataRow);
                    foundMatch = true;
                }
            }
            if (!foundMatch) {
                final DataRow nullDataRow = QueryResultUtils.createDataRowOfNullValues(rightQueryResult);
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, nullDataRow, queryResult.seeVariableDescriptors());
                queryResult.addRow(joinedDataRow);
            }
        }
        return queryResult;
    }


    SqlHeuristicResult calculateHeuristicQuery(Statement query) {
        if (SqlParserUtils.isUnion(query)) {
            List<Select> subqueries = SqlParserUtils.getUnionSubqueries(query);
            return calculateHeuristicUnion(subqueries);
        } else {
            final FromItem fromItem = getFrom(query);
            final List<Join> joins = getJoins(query);
            final Expression whereClause = getWhere(query);

            if (whereClause == null) {
                /**
                 * Handle case of SELECT/FROM without WHERE clause
                 */
                return calculateHeuristicRowSet(fromItem, joins);
            } else {
                /**
                 * Handle caseof SELECT/FROM with WHERE clause
                 */
                SqlHeuristicResult rowSetResult = calculateHeuristicRowSet(fromItem, joins);
                QueryResult rowSet = rowSetResult.getQueryResult();
                SqlHeuristicResult conditionResult = calculateHeuristicCondition(whereClause, rowSet);
                Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(rowSetResult.getTruthness(), conditionResult.getTruthness());
                return new SqlHeuristicResult(truthness, conditionResult.getQueryResult());
            }
        }
    }

    private SqlHeuristicResult calculateHeuristicUnion(List<Select> subqueries) {
        List<SqlHeuristicResult> subqueryResults = new ArrayList<>();
        for (Select subquery : subqueries) {
            SqlHeuristicResult subqueryResult = calculateHeuristicQuery(subquery);
            subqueryResults.add(subqueryResult);
        }
        final Truthness[] truthnesses = subqueryResults.stream()
                .map(SqlHeuristicResult::getTruthness)
                .toArray(Truthness[]::new);
        Truthness t = TruthnessUtils.buildOrAggregationTruthness(truthnesses);

        List<QueryResult> queryResults = subqueryResults.stream()
                .map(SqlHeuristicResult::getQueryResult)
                .collect(Collectors.toList());
        final QueryResult unionRowSet = QueryResultUtils.createUnionRowSet(queryResults);

        return new SqlHeuristicResult(t, unionRowSet);
    }

    /**
     * Calculates the heuristic result for a SELECT with FROM/WHERE clauses
     *
     * @return
     */
    private SqlHeuristicResult calculateHeuristicCondition(Expression condition, QueryResult rowSet) {
        Objects.requireNonNull(condition);
        Objects.requireNonNull(rowSet);
        return calculateHeuristicCondition(Collections.singletonList(condition), rowSet);
    }

    /**
     * Calculates the heuristic result for a SELECT with FROM/WHERE clauses
     *
     * @return
     */
    private SqlHeuristicResult calculateHeuristicCondition(Collection<Expression> conditions, QueryResult rowSet) {
        Objects.requireNonNull(conditions);
        Objects.requireNonNull(rowSet);

        double maxOfTrue = 0.0d;
        if (rowSet.isEmpty()) {
            return new SqlHeuristicResult(FALSE_TRUTHNESS, rowSet);
        } else {
            QueryResult filteredQueryResult = new QueryResult(rowSet.seeVariableDescriptors());
            for (DataRow row : rowSet.seeRows()) {
                Truthness truthnessForRow = evaluateAllConditions(conditions, row);
                if (truthnessForRow.isTrue()) {
                    filteredQueryResult.addRow(row);
                } else if (truthnessForRow.getOfTrue() > maxOfTrue) {
                    maxOfTrue = truthnessForRow.getOfTrue();
                }
            }
            final Truthness truthness;
            if (!filteredQueryResult.isEmpty()) {
                truthness = TRUE_TRUTHNESS;
            } else {
                truthness = TruthnessUtils.buildScaledTruthness(C, maxOfTrue);
            }
            return new SqlHeuristicResult(truthness, filteredQueryResult);
        }
    }

    private Truthness evaluateAllConditions(Collection<Expression> conditions, DataRow row) {
        List<Truthness> truthnesses = new ArrayList<>();
        for (Expression condition : conditions) {
            SqlExpressionEvaluator expressionEvaluator = new SqlExpressionEvaluator(sqlNameContext, taintHandler, row);
            condition.accept(expressionEvaluator);
            truthnesses.add(expressionEvaluator.getEvaluatedTruthness());
        }
        return TruthnessUtils.buildAndAggregationTruthness(truthnesses.toArray(new Truthness[0]));
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
