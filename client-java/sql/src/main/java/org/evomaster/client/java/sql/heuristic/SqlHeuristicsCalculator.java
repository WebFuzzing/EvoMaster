package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;
import org.evomaster.client.java.sql.VariableDescriptor;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
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

    private static QueryResultSet buildQueryResultSet(QueryResult... data) {
        QueryResultSet queryResultSet = new QueryResultSet();
        for (QueryResult queryResult : data) {
            queryResultSet.addQueryResult(queryResult);
        }
        return queryResultSet;
    }

    private final SqlExpressionEvaluator parentExpressionEvaluator;
    private final QueryResultSet queryResultSet;
    private final TaintHandler taintHandler;
    private final TableColumnResolver tableColumnResolver;
    private final Deque<DataRow> stackOfDataRows = new ArrayDeque<>();

    SqlHeuristicsCalculator(SqlExpressionEvaluator parentExpressionEvaluator,
                            TableColumnResolver tableColumnResolver,
                            TaintHandler taintHandler,
                            QueryResultSet queryResultSet,
                            Deque<DataRow> stackOfDataRows) {
        this.parentExpressionEvaluator = parentExpressionEvaluator;
        this.tableColumnResolver = tableColumnResolver;
        this.taintHandler = taintHandler;
        this.queryResultSet = queryResultSet;
        if (stackOfDataRows != null) {
            this.stackOfDataRows.addAll(stackOfDataRows);
        }
    }

    SqlHeuristicsCalculator(TableColumnResolver tableColumnResolver,
                            TaintHandler taintHandler,
                            QueryResultSet queryResultSet) {
        this(null, tableColumnResolver, taintHandler, queryResultSet, null);
    }


    public SqlHeuristicsCalculator(DbInfoDto schema, TaintHandler taintHandler, QueryResult... data) {
        this(new TableColumnResolver(schema), taintHandler, buildQueryResultSet(data));
    }

    public SqlHeuristicsCalculator(DbInfoDto schema, TaintHandler taintHandler, QueryResultSet queryResultSet) {
        this(new TableColumnResolver(schema), taintHandler, queryResultSet);
    }

    public static boolean isValidSqlCommandForSqlHeuristicsCalculation(String sqlCommand) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        return isValidSqlCommandForSqlHeuristicsCalculation(parsedSqlCommand);
    }

    public static boolean isValidSqlCommandForSqlHeuristicsCalculation(Statement statement) {
        return (statement instanceof Update)
                || (statement instanceof Delete)
                || (statement instanceof Select);
    }

    public SqlExpressionEvaluator getParentExpressionEvaluator() {
        return parentExpressionEvaluator;
    }

    public SqlDistanceWithMetrics computeDistance(String sqlCommand) {
        Objects.requireNonNull(sqlCommand, "sqlCommand cannot be null");

        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        if (!isValidSqlCommandForSqlHeuristicsCalculation(parsedSqlCommand)) {
            throw new IllegalArgumentException("Cannot compute heuristics for SQL command of type " + parsedSqlCommand.getClass().getName());
        }
        Truthness t;
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
            throw new IllegalArgumentException("Cannot compute heuristics for SQL command of type " + parsedSqlCommand.getClass().getName());
        }
        double distanceToTrue = 1 - t.getOfTrue();
        return new SqlDistanceWithMetrics(distanceToTrue, 0, false);
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
            QueryResult queryResult = queryResultSet.getQueryResultForVirtualTable() == null
                    ? new QueryResult(Collections.emptyList())
                    : queryResultSet.getQueryResultForVirtualTable();

            return new SqlHeuristicResult(TRUE_TRUTHNESS, queryResult);
        }

        if (SqlParserUtils.isTable(fromItem)) {
            /**
             * Handles the case where the FROM is a table
             */
            final QueryResult tableContents = createQueryResult(fromItem);
            final int len = tableContents.size();
            final Truthness truthness = TruthnessUtils.getTruthnessToEmpty(len).invert();
            return new SqlHeuristicResult(truthness, tableContents);
        }

        if (SqlParserUtils.isSubquery(fromItem)) {
            /**
             * Handles the case when FROM is a subquery
             */
            final Select subquery = SqlParserUtils.getSubquery(fromItem);
            return computeHeuristic(subquery);
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
        if (select instanceof SetOperationList) {
            SetOperationList unionQuery = (SetOperationList) select;
            List<Select> subqueries = unionQuery.getSelects();
            heuristicResult = computeHeuristicUnion(subqueries);
        } else if (select instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            Select subquery = parenthesedSelect.getSelect();
            heuristicResult = computeHeuristic(subquery);
        } else if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            final FromItem fromItem = getFrom(plainSelect);
            final List<Join> joins = getJoins(plainSelect);
            final Expression whereClause = getWhere(plainSelect);
            final SqlHeuristicResult intermediateHeuristicResult = computeHeuristic(fromItem, joins, whereClause);
            QueryResult queryResult = createQueryResult(intermediateHeuristicResult.getQueryResult(), plainSelect.getSelectItems());
            heuristicResult = new SqlHeuristicResult(intermediateHeuristicResult.getTruthness(), queryResult);
        } else {
            throw new IllegalArgumentException("Cannot calculate heuristics for SQL command of type " + select.getClass().getName());
        }
        tableColumnResolver.exitCurrentStatementContext();
        return heuristicResult;
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
        } else {
            for (DataRow row : queryResult.seeRows()) {
                final List<Object> filteredValues = evaluate(selectItems, row);
                DataRow filteredRow = new DataRow(variableDescriptors, filteredValues);
                filteredQueryResult.addRow(filteredRow);
            }
        }
        return filteredQueryResult;
    }

    private List<Object> evaluate(List<SelectItem<?>> selectItems, DataRow row) {
        List<Object> concreteValues = new ArrayList<>();
        for (SelectItem<?> selectItem : selectItems) {
            if (selectItem.getExpression() instanceof AllTableColumns) {
                AllTableColumns allTableColumns = (AllTableColumns) selectItem.getExpression();
                String tableName = allTableColumns.getTable().getName();
                for (VariableDescriptor vd : row.getVariableDescriptors()) {
                    if (tableName.equalsIgnoreCase(vd.getAliasTableName())
                            || tableName.equalsIgnoreCase(vd.getTableName())) {
                        final Object value = row.getValueByName(vd.getColumnName(), vd.getTableName(), vd.getAliasTableName());
                        concreteValues.add(value);
                    }
                }

            } else if (selectItem.getExpression() instanceof AllColumns) {
                concreteValues.addAll(row.seeValues());
            } else {
                Expression expression = selectItem.getExpression();
                final Object value = evaluate(expression, row);
                concreteValues.add(value);
            }
        }
        return concreteValues;
    }

    private Object evaluate(Expression expressionToEvaluate) {
        return evaluate(expressionToEvaluate, null);
    }

    private Object evaluate(Expression expressionToEvaluate, DataRow currentDataRow) {
        SqlExpressionEvaluator sqlExpressionEvaluator = new SqlExpressionEvaluator(
                this,
                this.tableColumnResolver,
                this.taintHandler,
                this.queryResultSet,
                this.stackOfDataRows,
                currentDataRow);
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
     * Notice that, * and table.* include table columns.
     *
     * @param selectItems
     * @return
     */
    private static boolean hasAnyTableColumn(List<SelectItem<?>> selectItems) {
        boolean hasAnyColumn = false;
        for (SelectItem<?> selectItem : selectItems) {
            if (selectItem.getExpression() instanceof AllTableColumns
                    || selectItem.getExpression() instanceof AllColumns
                    || selectItem.getExpression() instanceof Column) {
                hasAnyColumn = true;
            }
        }
        return hasAnyColumn;
    }

    private List<VariableDescriptor> createSelectVariableDescriptors(List<SelectItem<?>> selectItems, List<VariableDescriptor> variableDescriptors) {
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

    private SqlHeuristicResult computeHeuristicUnion(List<Select> subqueries) {
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
        final QueryResult unionRowSet = QueryResultUtils.createUnionRowSet(queryResults);

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

    private Truthness evaluateAll(Collection<Expression> conditions, DataRow row) {
        Objects.requireNonNull(row);
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("Cannot evaluate empty conditions");
        }

        List<Truthness> truthnesses = new ArrayList<>();
        for (Expression condition : conditions) {

            SqlExpressionEvaluator expressionEvaluator = new SqlExpressionEvaluator(this,
                    this.tableColumnResolver,
                    this.taintHandler,
                    this.queryResultSet,
                    this.stackOfDataRows,
                    row
            );
            condition.accept(expressionEvaluator);
            truthnesses.add(expressionEvaluator.getEvaluatedTruthness());
        }
        return TruthnessUtils.buildAndAggregationTruthness(truthnesses.toArray(new Truthness[0]));
    }

    private QueryResult createQueryResult(FromItem fromItem) {
        final QueryResult tableData;
        if (fromItem == null) {
            tableData = queryResultSet.getQueryResultForVirtualTable();
        } else {
            if (!SqlParserUtils.isTable(fromItem)) {
                throw new IllegalArgumentException("Cannot compute Truthness for form item that it is not a table " + fromItem);
            }
            String tableName = SqlParserUtils.getTableName(fromItem);

            if (fromItem.getAlias() != null) {
                tableData = QueryResultUtils.addAliasToQueryResult(queryResultSet.getQueryResultForNamedTable(tableName), fromItem.getAlias().getName());
            } else {
                tableData = queryResultSet.getQueryResultForNamedTable(tableName);
            }
        }
        return tableData;
    }


}
