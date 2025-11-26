package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.*;
import org.evomaster.client.java.sql.heuristic.function.FunctionFinder;
import org.evomaster.client.java.sql.heuristic.function.SqlAggregateFunction;
import org.evomaster.client.java.sql.heuristic.function.SqlFunction;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.SqlTableId;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.sql.internal.SqlParserUtils.*;

public class SqlHeuristicsCalculator {

    public static double C = 0.1;
    public static double C_BETTER = C + (C / 2);
    public static Truthness TRUE_TRUTHNESS = new Truthness(1, C);
    public static Truthness FALSE_TRUTHNESS = TRUE_TRUTHNESS.invert();
    public static Truthness FALSE_TRUTHNESS_BETTER = new Truthness(C_BETTER, 1);

    private final SqlExpressionEvaluator parentExpressionEvaluator;
    private final QueryResultSet sourceQueryResultSet;
    private final TaintHandler taintHandler;
    private final TableColumnResolver tableColumnResolver;
    private final Deque<DataRow> stackOfEvaluatedDataRows = new ArrayDeque<>();

    // Refactor: Use Builder Pattern for Constructor
    private SqlHeuristicsCalculator(SqlHeuristicsCalculatorBuilder builder) {
        this.parentExpressionEvaluator = builder.parentExpressionEvaluator;
        this.tableColumnResolver = builder.tableColumnResolver;
        this.taintHandler = builder.taintHandler;
        if (builder.queryResultSet != null) {
            this.sourceQueryResultSet = builder.queryResultSet;
        } else {
            this.sourceQueryResultSet = new QueryResultSet();
        }
        if (builder.stackOfDataRows != null) {
            this.stackOfEvaluatedDataRows.addAll(builder.stackOfDataRows);
        }
    }

    public static class SqlHeuristicsCalculatorBuilder {
        private SqlExpressionEvaluator parentExpressionEvaluator;
        private TableColumnResolver tableColumnResolver;
        private TaintHandler taintHandler;
        private QueryResultSet queryResultSet;
        private Deque<DataRow> stackOfDataRows;

        public SqlHeuristicsCalculatorBuilder withParentExpressionEvaluator(SqlExpressionEvaluator evaluator) {
            this.parentExpressionEvaluator = evaluator;
            return this;
        }

        public SqlHeuristicsCalculatorBuilder withTableColumnResolver(TableColumnResolver resolver) {
            this.tableColumnResolver = resolver;
            return this;
        }

        public SqlHeuristicsCalculatorBuilder withTaintHandler(TaintHandler handler) {
            this.taintHandler = handler;
            return this;
        }

        public SqlHeuristicsCalculatorBuilder withSourceQueryResultSet(QueryResultSet resultSet) {
            this.queryResultSet = resultSet;
            return this;
        }

        public SqlHeuristicsCalculatorBuilder withStackOfDataRows(Deque<DataRow> rows) {
            this.stackOfDataRows = rows;
            return this;
        }

        public SqlHeuristicsCalculator build() {
            return new SqlHeuristicsCalculator(this);
        }

    }

    public static boolean isValidSqlCommandForSqlHeuristicsCalculation(String sqlCommand) {
        return isValidSqlCommandForSqlHeuristicsCalculation(SqlParserUtils.parseSqlCommand(sqlCommand));
    }

    public static boolean isValidSqlCommandForSqlHeuristicsCalculation(Statement statement) {
        return (statement instanceof Update)
                || (statement instanceof Delete)
                || (statement instanceof Select);
    }

    public SqlDistanceWithMetrics computeDistance(String sqlCommand) {
        Objects.requireNonNull(sqlCommand, "sqlCommand cannot be null");
        try {
            Truthness t;
            Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
            if (parsedSqlCommand instanceof Select) {
                Select select = (Select) parsedSqlCommand;
                t = this.computeHeuristic(select).getTruthness();
            } else if (parsedSqlCommand instanceof Update) {
                Update update = (Update) parsedSqlCommand;
                t = this.computeHeuristic(update).getTruthness();
            } else if (parsedSqlCommand instanceof Delete) {
                Delete delete = (Delete) parsedSqlCommand;
                t = this.computeHeuristic(delete).getTruthness();
            } else {
                SimpleLogger.uniqueWarn("Cannot compute SQL complete heuristics for statement subclass: " + parsedSqlCommand.getClass().getName());
                return new SqlDistanceWithMetrics(Double.MAX_VALUE, 0, true);
            }
            double distanceToTrue = 1.0d - t.getOfTrue();
            return new SqlDistanceWithMetrics(distanceToTrue, 0, false);
        } catch (Exception ex) {
            SimpleLogger.uniqueWarn("Failed to compute complete SQL heuristics for: " + sqlCommand);
            return new SqlDistanceWithMetrics(Double.MAX_VALUE, 0, true);
        }
    }

    private SqlHeuristicResult computeHeuristicStatement(Statement statement) {
        tableColumnResolver.enterStatementeContext(statement);
        final SqlHeuristicResult heuristicResult = computeHeuristic(
                getFrom(statement),
                getJoins(statement),
                getWhere(statement));
        tableColumnResolver.exitCurrentStatementContext();
        return heuristicResult;
    }

    SqlHeuristicResult computeHeuristic(Update update) {
        return computeHeuristicStatement(update);
    }

    SqlHeuristicResult computeHeuristic(Delete delete) {
        return computeHeuristicStatement(delete);
    }

    private SqlHeuristicResult computeHeuristic(FromItem fromItem) {

        if (fromItem == null) {
            /**
             * Handle the case where no FROM is used (e.g. SELECT 42;)
             */
            return new SqlHeuristicResult(TRUE_TRUTHNESS, new QueryResult(Collections.emptyList()));
        }

        if (fromItem instanceof Table) {
            /**
             * Handles the case where the FROM is a table
             */
            final QueryResult tableContents = createQueryResult(fromItem);
            final int len = tableContents.size();
            final Truthness truthness = TruthnessUtils.getTruthnessToEmpty(len).invert();
            return new SqlHeuristicResult(truthness, tableContents);
        }

        if (fromItem instanceof ParenthesedSelect) {
            /**
             * Handles the case when FROM is a subquery
             */
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            final Select subquery = parenthesedSelect.getSelect();
            return computeHeuristic(subquery);
        }

        if (fromItem instanceof TableFunction) {
            throw new UnsupportedOperationException("Must implement TableFunction for computing heuristics");
        }

        if (fromItem instanceof ParenthesedFromItem) {
            throw new UnsupportedOperationException("Must implement ParenthesedFromItem for computing heuristics");
        }

        throw new IllegalArgumentException("Cannot compute heuristics for FROM item of type " + fromItem.getClass().getName());
    }

    private SqlHeuristicResult computeHeuristic(FromItem fromItem, List<Join> joins) {

        SqlHeuristicResult fromItemResult = computeHeuristic(fromItem);

        if (joins != null && !joins.isEmpty()) {
            SqlHeuristicResult joinResult = fromItemResult;
            for (Join join : joins) {
                if (join.isInnerJoin()) {
                    joinResult = computeHeuristicInnerJoin(joinResult, join);
                } else if (join.isLeft()) {
                    joinResult = computeHeuristicLeftJoin(joinResult, join);
                } else if (join.isRight()) {
                    joinResult = computeHeuristicRightJoin(joinResult, join);
                } else if (join.isCross()) {
                    joinResult = computeHeuristicCrossJoin(joinResult, join);
                } else if (join.isFull()) {
                    joinResult = computeHeuristicFullJoin(joinResult, join);
                } else {
                    throw new IllegalArgumentException("Join type not supported: " + join);
                }
            }
            return joinResult;

        } else {
            return fromItemResult;
        }
    }

    private SqlHeuristicResult computeHeuristicFullJoin(SqlHeuristicResult leftRowSetResult, Join fullJoin) {
        if (!fullJoin.isFull()) {
            throw new IllegalArgumentException("Join is not a FULL JOIN");
        }

        final FromItem rightFromItem = fullJoin.getRightItem();
        final Collection<Expression> onExpressions = fullJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = computeHeuristic(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();

        QueryResult fullJoinQueryResult = createQueryResultFullJoin(leftQueryResult, rightQueryResult, onExpressions);

        Truthness truthness = TruthnessUtils.buildOrAggregationTruthness(leftRowSetResult.getTruthness(),
                rightRowSetResult.getTruthness());

        return new SqlHeuristicResult(truthness, fullJoinQueryResult);
    }

    private SqlHeuristicResult computeHeuristicCrossJoin(SqlHeuristicResult leftRowSetResult, Join crossJoin) {
        if (!crossJoin.isCross()) {
            throw new IllegalArgumentException("Join is not a CROSS JOIN");
        }

        final FromItem rightFromItem = crossJoin.getRightItem();
        SqlHeuristicResult rightRowSetResult = computeHeuristic(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();
        final QueryResult cartesianProduct = QueryResultUtils.createCartesianProduct(leftQueryResult, rightQueryResult);

        Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(leftRowSetResult.getTruthness(),
                rightRowSetResult.getTruthness());

        return new SqlHeuristicResult(truthness, cartesianProduct);
    }


    private SqlHeuristicResult computeHeuristicInnerJoin(SqlHeuristicResult leftRowSetResult, Join innerJoin) {
        if (!innerJoin.isInnerJoin()) {
            throw new IllegalArgumentException("Join is not a INNER JOIN");
        }

        final FromItem rightFromItem = innerJoin.getRightItem();
        final Collection<Expression> onExpressions = innerJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = computeHeuristic(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();
        final QueryResult cartesianProduct = QueryResultUtils.createCartesianProduct(leftQueryResult, rightQueryResult);

        SqlHeuristicResult conditionResult = computeHeuristic(cartesianProduct, onExpressions);

        Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(leftRowSetResult.getTruthness(),
                rightRowSetResult.getTruthness(),
                conditionResult.getTruthness());
        return new SqlHeuristicResult(truthness, conditionResult.getQueryResult());
    }

    private SqlHeuristicResult computeHeuristicRightJoin(SqlHeuristicResult leftRowSetResult, Join rightJoin) {
        if (!rightJoin.isRight()) {
            throw new IllegalArgumentException("Join is not a RIGHT JOIN");
        }

        final FromItem rightFromItem = rightJoin.getRightItem();
        final Collection<Expression> onExpressions = rightJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = computeHeuristic(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();

        final QueryResult queryResult = createQueryResultRightJoin(leftQueryResult, rightQueryResult, onExpressions);

        return new SqlHeuristicResult(rightRowSetResult.getTruthness(), queryResult);
    }

    private QueryResult createQueryResultRightJoin(QueryResult leftQueryResult, QueryResult
            rightQueryResult, Collection<Expression> onExpressions) {
        final QueryResult queryResult = QueryResultUtils.createEmptyCartesianProduct(leftQueryResult, rightQueryResult);
        for (DataRow rightRow : rightQueryResult.seeRows()) {
            boolean foundMatch = false;
            for (DataRow leftRow : leftQueryResult.seeRows()) {
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, rightRow, queryResult.seeVariableDescriptors());
                final Truthness truthness = onExpressions.isEmpty()
                        ? TRUE_TRUTHNESS
                        : evaluateAll(onExpressions, joinedDataRow);
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

    private SqlHeuristicResult computeHeuristicLeftJoin(SqlHeuristicResult leftRowSetResult, Join leftJoin) {
        if (!leftJoin.isLeft()) {
            throw new IllegalArgumentException("Join is not a LEFT JOIN");
        }

        final FromItem rightFromItem = leftJoin.getRightItem();
        final Collection<Expression> onExpressions = leftJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = computeHeuristic(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();

        final QueryResult queryResult = createQueryResultLeftJoin(leftQueryResult, rightQueryResult, onExpressions);

        return new SqlHeuristicResult(leftRowSetResult.getTruthness(), queryResult);
    }

    private QueryResult createQueryResultFullJoin(QueryResult leftQueryResult,
                                                  QueryResult rightQueryResult,
                                                  Collection<Expression> onExpressions) {

        final QueryResult queryResult = QueryResultUtils.createEmptyCartesianProduct(leftQueryResult, rightQueryResult);

        boolean[] rightRowMatched = new boolean[rightQueryResult.size()];
        for (DataRow leftRow : leftQueryResult.seeRows()) {
            boolean foundMatch = false;
            for (int i = 0; i < rightQueryResult.seeRows().size(); i++) {
                DataRow rightRow = rightQueryResult.seeRows().get(i);
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, rightRow, queryResult.seeVariableDescriptors());
                final Truthness truthness = onExpressions.isEmpty()
                        ? TRUE_TRUTHNESS
                        : evaluateAll(onExpressions, joinedDataRow);
                if (truthness.isTrue()) {
                    queryResult.addRow(joinedDataRow);
                    foundMatch = true;
                    rightRowMatched[i] = true;
                }
            }
            if (!foundMatch) {
                final DataRow nullDataRow = QueryResultUtils.createDataRowOfNullValues(rightQueryResult);
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, nullDataRow, queryResult.seeVariableDescriptors());
                queryResult.addRow(joinedDataRow);
            }
        }

        for (int i = 0; i < rightRowMatched.length; i++) {
            if (!rightRowMatched[i]) {
                DataRow rightRow = rightQueryResult.seeRows().get(i);
                final DataRow nullDataRow = QueryResultUtils.createDataRowOfNullValues(leftQueryResult);
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(nullDataRow, rightRow, queryResult.seeVariableDescriptors());
                queryResult.addRow(joinedDataRow);
            }
        }

        return queryResult;
    }


    private QueryResult createQueryResultLeftJoin(QueryResult leftQueryResult, QueryResult
            rightQueryResult, Collection<Expression> onExpressions) {
        final QueryResult queryResult = QueryResultUtils.createEmptyCartesianProduct(leftQueryResult, rightQueryResult);
        for (DataRow leftRow : leftQueryResult.seeRows()) {
            boolean foundMatch = false;
            for (DataRow rightRow : rightQueryResult.seeRows()) {
                final DataRow joinedDataRow = QueryResultUtils.createJoinedRow(leftRow, rightRow, queryResult.seeVariableDescriptors());
                final Truthness truthness = onExpressions.isEmpty()
                        ? TRUE_TRUTHNESS
                        : evaluateAll(onExpressions, joinedDataRow);
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


    SqlHeuristicResult computeHeuristic(Select select) {
        tableColumnResolver.enterStatementeContext(select);
        final SqlHeuristicResult heuristicResult;
        if (select.getLimit() != null && getLimitValue(select.getLimit()) == 0) {
            // Handle case of LIMIT 0
            heuristicResult = new SqlHeuristicResult(FALSE_TRUTHNESS, new QueryResult(Collections.emptyList()));
        } else {
            if (select instanceof SetOperationList) {
                // Handle case of UNION/INTERSECT/EXCEPT
                SetOperationList unionQuery = (SetOperationList) select;
                List<Select> subqueries = unionQuery.getSelects();
                heuristicResult = computeHeuristicUnion(subqueries, unionQuery.getLimit());
            } else if (select instanceof ParenthesedSelect) {
                // Handle case of parenthesed SELECT
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
                Select subquery = parenthesedSelect.getSelect();
                final SqlHeuristicResult subqueryHeuristicResult = computeHeuristic(subquery);
                if (parenthesedSelect.getLimit() != null) {
                    long limitValue = getLimitValue(parenthesedSelect.getLimit());
                    QueryResult queryResult = subqueryHeuristicResult.getQueryResult().limit(limitValue);
                    heuristicResult = new SqlHeuristicResult(subqueryHeuristicResult.getTruthness(), queryResult);
                } else {
                    heuristicResult = subqueryHeuristicResult;
                }
            } else if (select instanceof PlainSelect) {
                // Handle case of plain SELECT
                PlainSelect plainSelect = (PlainSelect) select;
                final FromItem fromItem = getFrom(plainSelect);
                final List<Join> joins = getJoins(plainSelect);
                final Expression whereClause = getWhere(plainSelect);
                if (plainSelect.getGroupBy() != null) {
                    heuristicResult = computeHeuristicSelectGroupByHaving(
                            plainSelect.getSelectItems(),
                            fromItem,
                            joins,
                            whereClause,
                            plainSelect.getGroupBy().getGroupByExpressionList(),
                            plainSelect.getHaving(),
                            plainSelect.getOrderByElements(),
                            plainSelect.getLimit());
                } else {
                    heuristicResult = computeHeuristicSelect(
                            plainSelect.getSelectItems(),
                            fromItem,
                            joins,
                            whereClause,
                            plainSelect.getOrderByElements(),
                            plainSelect.getLimit());
                }
            } else {
                throw new IllegalArgumentException("Cannot calculate heuristics for SQL command of type " + select.getClass().getName());
            }
        }
        tableColumnResolver.exitCurrentStatementContext();
        return heuristicResult;
    }

    private SqlHeuristicResult computeHeuristicSelect(List<SelectItem<?>> selectItems, FromItem fromItem, List<Join> joins, Expression whereClause) {
        final SqlHeuristicResult intermediateHeuristicResult = computeHeuristic(fromItem, joins, whereClause);
        final QueryResult queryResult = createQueryResult(intermediateHeuristicResult.getQueryResult(), selectItems);
        final SqlHeuristicResult heuristicResult = new SqlHeuristicResult(intermediateHeuristicResult.getTruthness(), queryResult);
        return heuristicResult;
    }

    private SqlHeuristicResult computeHeuristicSelect(List<SelectItem<?>> selectItems,
                                                      FromItem fromItem,
                                                      List<Join> joins,
                                                      Expression whereClause,
                                                      Limit limit) {
        final SqlHeuristicResult intermediateHeuristicResult = computeHeuristic(fromItem, joins, whereClause);
        QueryResult queryResult = createQueryResult(intermediateHeuristicResult.getQueryResult(), selectItems);

        if (limit != null) {
            long limitValue = getLimitValue(limit);
            queryResult = queryResult.limit(limitValue);
        }

        final SqlHeuristicResult heuristicResult = new SqlHeuristicResult(intermediateHeuristicResult.getTruthness(), queryResult);
        return heuristicResult;
    }

    private SqlHeuristicResult computeHeuristicSelect(List<SelectItem<?>> selectItems,
                                                      FromItem fromItem,
                                                      List<Join> joins,
                                                      Expression whereClause,
                                                      List<OrderByElement> orderByElements,
                                                      Limit limit) {
        final SqlHeuristicResult intermediateHeuristicResult = computeHeuristic(fromItem, joins, whereClause);
        QueryResult queryResult = createQueryResult(intermediateHeuristicResult.getQueryResult(), selectItems);

        if (orderByElements != null && !orderByElements.isEmpty()) {
            queryResult = createQueryResultOrderBy(queryResult, orderByElements);
        }

        if (limit != null) {
            long limitValue = getLimitValue(limit);
            queryResult = queryResult.limit(limitValue);
        }

        final SqlHeuristicResult heuristicResult = new SqlHeuristicResult(intermediateHeuristicResult.getTruthness(), queryResult);
        return heuristicResult;
    }

    private SqlHeuristicResult computeHeuristicSelectGroupByHaving(
            List<SelectItem<?>> selectItems,
            FromItem fromItem,
            List<Join> joins,
            Expression whereClause,
            List<Expression> groupByExpressions,
            Expression having,
            List<OrderByElement> orderByElements,
            Limit limit) {

        final SqlHeuristicResult intermediateHeuristicResult = computeHeuristic(fromItem, joins, whereClause);

        QueryResult sourceQueryResult = intermediateHeuristicResult.getQueryResult();

        Map<List<Object>, QueryResult> groupByQueryResults = new HashMap<>();
        for (DataRow dataRow : sourceQueryResult.seeRows()) {
            List<Object> key = new ArrayList<>();
            for (Expression groupByExpression : groupByExpressions) {
                Object value = evaluate(groupByExpression, dataRow);
                key.add(value);
            }
            groupByQueryResults.computeIfAbsent(key, k -> new QueryResult(sourceQueryResult.seeVariableDescriptors()))
                    .addRow(dataRow);
        }

        final List<DataRow> groupByDataRows = new ArrayList<>();
        final List<Truthness> truthnesses = new ArrayList<>();
        for (QueryResult groupByQueryResult : groupByQueryResults.values()) {
            QueryResult aggregatedQueryResult = createQueryResult(groupByQueryResult, selectItems);
            if (aggregatedQueryResult.size() != 1) {
                throw new IllegalStateException("An aggregated query result cannot have " + aggregatedQueryResult.size() + "rows");
            }
            DataRow dataRow = aggregatedQueryResult.seeRows().get(0);
            if (having != null) {
                final Truthness truthness = evaluateAll(Collections.singletonList(having), groupByQueryResult);
                truthnesses.add(truthness);
                if (truthness.isTrue()) {
                    groupByDataRows.add(dataRow);
                }
            } else {
                groupByDataRows.add(dataRow);
            }
        }
        final List<VariableDescriptor> variableDescriptors = createSelectVariableDescriptors(selectItems, sourceQueryResult.seeVariableDescriptors());
        QueryResult queryResult = new QueryResult(variableDescriptors);
        for (DataRow groupByDataRow : groupByDataRows) {
            queryResult.addRow(groupByDataRow);
        }
        final Truthness havingTruthness;
        if (truthnesses.isEmpty()) {
            havingTruthness = SqlHeuristicsCalculator.TRUE_TRUTHNESS;
        } else {
            havingTruthness = TruthnessUtils.buildOrAggregationTruthness(truthnesses.toArray(new Truthness[]{}));
        }

        final Truthness groupByHavingTruthness = TruthnessUtils.buildAndAggregationTruthness(
                intermediateHeuristicResult.getTruthness(),
                havingTruthness);

        if (orderByElements != null && !orderByElements.isEmpty()) {
            queryResult = createQueryResultOrderBy(queryResult, orderByElements);
        }

        if (limit != null) {
            long limitValue = getLimitValue(limit);
            queryResult = queryResult.limit(limitValue);
        }

        return new SqlHeuristicResult(groupByHavingTruthness, queryResult);
    }

    private SqlHeuristicResult computeHeuristic(FromItem fromItem, List<Join> joins, Expression whereExpression) {
        SqlHeuristicResult heuristicResult = computeHeuristic(fromItem, joins);
        QueryResult queryResult = heuristicResult.getQueryResult();
        if (whereExpression == null) {
            /**
             * Handle case of SELECT/FROM without WHERE clause
             */
            return heuristicResult;
        } else {
            /**
             * Handle caseof SELECT/FROM with WHERE clause
             */
            SqlHeuristicResult conditionResult = computeHeuristic(queryResult, whereExpression);
            Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(heuristicResult.getTruthness(), conditionResult.getTruthness());
            return new SqlHeuristicResult(truthness, conditionResult.getQueryResult());
        }
    }

    private QueryResult createQueryResult(QueryResult queryResult, List<SelectItem<?>> selectItems) {
        if (selectItems == null || selectItems.isEmpty()) {
            return queryResult;
        }

        final List<VariableDescriptor> variableDescriptors = createSelectVariableDescriptors(selectItems, queryResult.seeVariableDescriptors());
        QueryResult filteredQueryResult = new QueryResult(variableDescriptors);

        if (queryResult.isEmpty() && !hasAnyTableColumn(selectItems)) {
            final List<Object> rowValues = evaluate(selectItems);
            DataRow singleRow = new DataRow(variableDescriptors, rowValues);
            filteredQueryResult.addRow(singleRow);
        } else if (hasAnyAggregateFunction(selectItems)) {
            DataRow witnessDataRow = queryResult.isEmpty()
                    ? null
                    : queryResult.seeRows().get(0);
            final List<Object> filteredValues = evaluate(selectItems, witnessDataRow, queryResult);
            DataRow filteredRow = new DataRow(variableDescriptors, filteredValues);
            filteredQueryResult.addRow(filteredRow);
        } else {
            for (DataRow row : queryResult.seeRows()) {
                final List<Object> filteredValues = evaluate(selectItems, row, queryResult);
                DataRow filteredRow = new DataRow(variableDescriptors, filteredValues);
                filteredQueryResult.addRow(filteredRow);
            }
        }

        return filteredQueryResult;
    }

    private List<Object> evaluate(List<SelectItem<?>> selectItems, DataRow currentDataRow, QueryResult currentQueryResult) {
        List<Object> values = new ArrayList<>();
        for (SelectItem<?> selectItem : selectItems) {
            Expression expression = selectItem.getExpression();
            final Object value = evaluate(expression, currentDataRow, currentQueryResult);
            if (value != null && value instanceof QueryResult) {
                QueryResult queryResult = (QueryResult) value;
                if (queryResult.isEmpty()) {
                    values.add(null);
                } else if (queryResult.seeRows().size() == 1) {
                    Object singleValue = queryResult.seeRows().get(0).getValue(0);
                    values.add(singleValue);
                } else {
                    throw new IllegalArgumentException("Cannot evaluate " + expression.toString() + " if resulting subquery size is greater than 1" + currentDataRow.toString());
                }
            } else if (value != null && value instanceof List<?>) {
                List<Object> evaluatedValues = (List<Object>) value;
                values.addAll(evaluatedValues);
            } else {
                values.add(value);
            }
        }
        return values;
    }

    private static boolean isAggregateFunction(String functionName) {
        final SqlFunction function = FunctionFinder.getInstance().getFunction(functionName);
        return function != null && function instanceof SqlAggregateFunction;
    }

    private Object evaluate(Expression expressionToEvaluate) {
        return evaluate(expressionToEvaluate, null, null);
    }

    private Object evaluate(Expression expressionToEvaluate, DataRow currentDataRow) {
        SqlExpressionEvaluator sqlExpressionEvaluator =
                new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                        .withParentStatementEvaluator(this)
                        .withTableColumnResolver(this.tableColumnResolver)
                        .withTaintHandler(this.taintHandler)
                        .withQueryResultSet(this.sourceQueryResultSet)
                        .withDataRowStack(this.stackOfEvaluatedDataRows)
                        .withCurrentDataRow(currentDataRow).build();

        expressionToEvaluate.accept(sqlExpressionEvaluator);
        Object value = sqlExpressionEvaluator.getEvaluatedValue();
        return value;
    }

    private Object evaluate(Expression expressionToEvaluate, DataRow currentDataRow, QueryResult currentQueryResult) {
        SqlExpressionEvaluator sqlExpressionEvaluator =
                new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                        .withParentStatementEvaluator(this)
                        .withTableColumnResolver(this.tableColumnResolver)
                        .withTaintHandler(this.taintHandler)
                        .withQueryResultSet(this.sourceQueryResultSet)
                        .withDataRowStack(this.stackOfEvaluatedDataRows)
                        .withCurrentDataRow(currentDataRow)
                        .withCurrentQueryResult(currentQueryResult)
                        .build();

        expressionToEvaluate.accept(sqlExpressionEvaluator);
        Object value = sqlExpressionEvaluator.getEvaluatedValue();
        return value;
    }


    private List<Object> evaluate(List<SelectItem<?>> selectItems) {
        List<Object> filteredValues = new ArrayList<>();
        for (SelectItem<?> selectItem : selectItems) {
            final Expression expression = selectItem.getExpression();
            final Object value = evaluate(expression);
            filteredValues.add(value);
        }
        return filteredValues;
    }

    /**
     * Checks if the select items contain any table column.
     * Notice that, allColumns (*) and allTableColumns (table.*) include table columns.
     *
     * @param selectItems
     * @return
     */
    private static boolean hasAnyTableColumn(List<SelectItem<?>> selectItems) {
        for (SelectItem<?> selectItem : selectItems) {
            if (hasAnyTableColumn(selectItem.getExpression())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyTableColumn(Expression expression) {
        if (expression instanceof AllTableColumns) {
            return true;
        }
        if (expression instanceof AllColumns) {
            return true;
        }
        if (expression instanceof Column) {
            return true;
        }
        if (expression instanceof Function) {
            Function functionExpression = (Function) expression;
            String functionName = functionExpression.getName();
            if (isAggregateFunction(functionName)) {
                for (int i = 0; i < functionExpression.getParameters().size(); i++) {
                    if (hasAnyTableColumn(functionExpression.getParameters().get(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private static boolean hasAnyAggregateFunction(List<SelectItem<?>> selectItems) {
        for (SelectItem<?> selectItem : selectItems) {
            if (selectItem.getExpression() instanceof Function
                    && isAggregateFunction(((Function) selectItem.getExpression()).getName())) {
                return true;
            }
        }
        return false;
    }

    private List<VariableDescriptor> createSelectVariableDescriptors
            (List<SelectItem<?>> selectItems, List<VariableDescriptor> variableDescriptors) {
        List<VariableDescriptor> selectVariableDescriptors = new ArrayList<>();

        for (SelectItem<?> selectItem : selectItems) {
            if (selectItem.getExpression() instanceof AllTableColumns) {
                AllTableColumns allTableColumns = (AllTableColumns) selectItem.getExpression();
                String tableName = allTableColumns.getTable().getName();
                for (VariableDescriptor variableDescriptor : variableDescriptors) {
                    if (tableName.equalsIgnoreCase(variableDescriptor.getAliasTableName())
                            || tableName.equalsIgnoreCase(variableDescriptor.getTableName())) {
                        selectVariableDescriptors.add(variableDescriptor);
                    }
                }

            } else if (selectItem.getExpression() instanceof AllColumns) {
                selectVariableDescriptors.addAll(variableDescriptors);

            } else if (selectItem.getExpression() instanceof Column) {
                Column column = (Column) selectItem.getExpression();
                SqlColumnReference columnReference = this.tableColumnResolver.resolve(column);
                String columnName = columnReference.getColumnName();
                String aliasName = selectItem.getAlias() != null ? selectItem.getAlias().getName() : columnName;
                String tableName;
                if (columnReference.getTableReference() instanceof SqlBaseTableReference) {
                    tableName = ((SqlBaseTableReference) columnReference.getTableReference()).getName();
                } else {
                    tableName = null;
                }
                VariableDescriptor variableDescriptor = new VariableDescriptor(columnName, aliasName, tableName);
                selectVariableDescriptors.add(variableDescriptor);
            } else {

                // create unnamed column
                String columnName = selectItem.getAlias() != null
                        ? selectItem.getAlias().getName()
                        : null;
                VariableDescriptor variableDescriptor = new VariableDescriptor(columnName);
                selectVariableDescriptors.add(variableDescriptor);
            }
        }
        return selectVariableDescriptors;
    }

    private SqlHeuristicResult computeHeuristicUnion(List<Select> subqueries, Limit limit) {
        List<SqlHeuristicResult> subqueryResults = new ArrayList<>();
        for (Select subquery : subqueries) {
            SqlHeuristicResult subqueryResult = computeHeuristic(subquery);
            subqueryResults.add(subqueryResult);
        }
        final Truthness[] truthnesses = subqueryResults.stream()
                .map(SqlHeuristicResult::getTruthness)
                .toArray(Truthness[]::new);
        Truthness t = TruthnessUtils.buildOrAggregationTruthness(truthnesses);

        List<QueryResult> queryResults = subqueryResults.stream()
                .map(SqlHeuristicResult::getQueryResult)
                .collect(Collectors.toList());
        QueryResult unionRowSet = QueryResultUtils.createUnionRowSet(queryResults);

        if (limit != null) {
            long limitValue = getLimitValue(limit);
            unionRowSet = unionRowSet.limit(limitValue);
        }

        return new SqlHeuristicResult(t, unionRowSet);
    }

    /**
     * Calculates the heuristic result for a SELECT with FROM/WHERE clauses
     *
     * @return
     */
    private SqlHeuristicResult computeHeuristic(QueryResult queryResult, Expression condition) {
        Objects.requireNonNull(condition);
        Objects.requireNonNull(queryResult);
        return computeHeuristic(queryResult, Collections.singletonList(condition));
    }

    /**
     * Calculates the heuristic result for a SELECT with FROM/WHERE clauses
     *
     * @return
     */
    private SqlHeuristicResult computeHeuristic(QueryResult queryResult, Collection<Expression> conditions) {
        Objects.requireNonNull(conditions);
        Objects.requireNonNull(queryResult);

        double maxOfTrue = 0.0d;
        if (queryResult.isEmpty()) {
            return new SqlHeuristicResult(FALSE_TRUTHNESS, queryResult);
        } else {
            QueryResult filteredQueryResult = new QueryResult(queryResult.seeVariableDescriptors());
            for (DataRow row : queryResult.seeRows()) {
                Truthness truthnessForRow = conditions.isEmpty()
                        ? TRUE_TRUTHNESS
                        : evaluateAll(conditions, row);
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

    private Truthness evaluateAll(Collection<Expression> conditions, QueryResult currentQueryResult) {
        Objects.requireNonNull(currentQueryResult);
        return evaluateAll(conditions, null, currentQueryResult);
    }

    private Truthness evaluateAll(Collection<Expression> conditions, DataRow currentDataRow, QueryResult currentQueryResult) {
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("Cannot evaluate empty conditions");
        }

        List<Truthness> truthnesses = new ArrayList<>();
        for (Expression condition : conditions) {

            SqlExpressionEvaluator expressionEvaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                    .withParentStatementEvaluator(this)
                    .withTableColumnResolver(this.tableColumnResolver)
                    .withTaintHandler(this.taintHandler)
                    .withQueryResultSet(this.sourceQueryResultSet)
                    .withDataRowStack(this.stackOfEvaluatedDataRows)
                    .withCurrentDataRow(currentDataRow)
                    .withCurrentQueryResult(currentQueryResult)
                    .build();
            condition.accept(expressionEvaluator);
            truthnesses.add(expressionEvaluator.getEvaluatedTruthness());
        }
        return TruthnessUtils.buildAndAggregationTruthness(truthnesses.toArray(new Truthness[0]));
    }


    private Truthness evaluateAll(Collection<Expression> conditions, DataRow row) {
        Objects.requireNonNull(row);
        return evaluateAll(conditions, row, null);
    }

    private QueryResult createQueryResultOrderBy(QueryResult queryResult, List<OrderByElement> orderByElements) {
        if (orderByElements == null || orderByElements.isEmpty()) {
            throw new IllegalArgumentException("Cannot evaluate empty orderBy elements");
        }

        QueryResult sortedResult = new QueryResult(queryResult.seeVariableDescriptors());

        List<DataRow> sortedRows = new ArrayList<>(queryResult.seeRows());
        sortedRows.sort(new OrderByComparator(orderByElements));

        for (DataRow row : sortedRows) {
            sortedResult.addRow(row);
        }

        return sortedResult;
    }

    private QueryResult createQueryResult(FromItem fromItem) {
        final QueryResult tableData;
        if (fromItem == null) {
            tableData = new QueryResult(Collections.emptyList());
        } else {
            if (!SqlParserUtils.isTable(fromItem)) {
                throw new IllegalArgumentException("Cannot compute Truthness for form item that it is not a table " + fromItem);
            }
            String tableName = SqlParserUtils.getTableName(fromItem);

            if (fromItem.getAlias() != null) {
                tableData = QueryResultUtils.addAliasToQueryResult(sourceQueryResultSet.getQueryResultForNamedTable(tableName), fromItem.getAlias().getName());
            } else {
                Table table = (Table) fromItem;
                if (this.tableColumnResolver.resolve(table)!=null) {
                    SqlTableReference sqlTableReference = this.tableColumnResolver.resolve(table);
                    if (sqlTableReference instanceof SqlBaseTableReference) {
                        SqlBaseTableReference sqlBaseTableReference = (SqlBaseTableReference) sqlTableReference;
                        SqlTableId sqlTableId = sqlBaseTableReference.getTableId();
                        tableData = sourceQueryResultSet.getQueryResultForNamedTable(sqlTableId.getTableId());
                    } else if (sqlTableReference instanceof SqlDerivedTableReference) {
                        SqlDerivedTableReference sqlDerivedTableReference = (SqlDerivedTableReference) sqlTableReference;
                        Select select = sqlDerivedTableReference.getSelect();
                        SqlHeuristicResult sqlHeuristicResult = this.computeHeuristic(select);
                        tableData = sqlHeuristicResult.getQueryResult();
                    } else {
                        throw new IllegalArgumentException("Cannot compute Truthness for form item that it is not a table " + table);
                    }

                } else {
                    tableData = sourceQueryResultSet.getQueryResultForNamedTable(tableName);
                }
            }
        }
        return tableData;
    }


    private class OrderByComparator implements Comparator<DataRow> {

        private final List<OrderByElement> orderByElements;

        public OrderByComparator(List<OrderByElement> orderByElements) {
            this.orderByElements = orderByElements;
        }

        @Override
        public int compare(DataRow r1, DataRow r2) {
            for (OrderByElement orderByElement : orderByElements) {
                Expression orderByElementExpression = orderByElement.getExpression();
                Object val1 = evaluate(orderByElementExpression, r1);
                Object val2 = evaluate(orderByElementExpression, r2);

                int comparison;
                if (val1 == null && val2 == null) {
                    comparison = 0;
                } else if (val1 == null) {
                    comparison = -1;
                } else if (val2 == null) {
                    comparison = 1;
                } else {
                    if (val1 instanceof Comparable && val2 instanceof Comparable) {
                        comparison = ((Comparable) val1).compareTo(val2);
                    } else {
                        // Cannot compare, treat as equal
                        comparison = 0;
                    }
                }

                if (comparison != 0) {
                    return orderByElement.isAsc() ? comparison : -comparison;
                }
            }
            return 0;
        }
    }


}
