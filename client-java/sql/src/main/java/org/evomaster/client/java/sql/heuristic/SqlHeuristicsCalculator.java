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
            t = this.calculateHeuristic(select).getTruthness();
        } else if (parsedSqlCommand instanceof Update) {
            Update update = (Update) parsedSqlCommand;
            t = this.calculateHeuristic(update).getTruthness();
        } else if (parsedSqlCommand instanceof Delete) {
            Delete delete = (Delete) parsedSqlCommand;
            t = this.calculateHeuristic(delete).getTruthness();
        } else {
            throw new IllegalArgumentException("Cannot compute heuristics for SQL command of type " + parsedSqlCommand.getClass().getName());
        }
        double distanceToTrue = 1 - t.getOfTrue();
        return new SqlDistanceWithMetrics(distanceToTrue, 0, false);
    }

    SqlHeuristicResult calculateHeuristic(Update update) {
        tableColumnResolver.enterStatementeContext(update);
        final FromItem fromItem = getFrom(update);
        final List<Join> joins = getJoins(update);
        final Expression whereClause = getWhere(update);

        final SqlHeuristicResult heuristicResult = calculateHeuristic(fromItem, joins, whereClause);
        tableColumnResolver.exitCurrentStatementContext();
        return heuristicResult;
    }

    SqlHeuristicResult calculateHeuristic(Delete delete) {
        tableColumnResolver.enterStatementeContext(delete);
        final FromItem fromItem = getFrom(delete);
        final List<Join> joins = getJoins(delete);
        final Expression whereClause = getWhere(delete);

        final SqlHeuristicResult heuristicResult = calculateHeuristic(fromItem, joins, whereClause);
        tableColumnResolver.exitCurrentStatementContext();
        return heuristicResult;
    }

    private SqlHeuristicResult calculateHeuristicRowSet(FromItem fromItem) {
        return calculateHeuristicRowSet(fromItem, null);
    }

    private SqlHeuristicResult calculateHeuristicRowSet(FromItem fromItem, List<Join> joins) {

        if (fromItem == null) {
            /**
             * Handle the case where no FROM is used (e.g. SELECT 42;)
             */
            QueryResult queryResult = queryResultSet.getQueryResultForVirtualTable() == null
                    ? new QueryResult(Collections.emptyList())
                    : queryResultSet.getQueryResultForVirtualTable();

            return new SqlHeuristicResult(TRUE_TRUTHNESS, queryResult);
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
            final Select subquery = SqlParserUtils.getSubquery(fromItem);
            return calculateHeuristic(subquery);
        }

        if (joins != null && !joins.isEmpty()) {
            SqlHeuristicResult joinResult = calculateHeuristicRowSet(fromItem);
            for (Join join : joins) {
                if (join.isInnerJoin()) {
                    joinResult = calculateHeuristicInnerJoin(joinResult, join);
                } else if (join.isLeft()) {
                    joinResult = calculateHeuristicLeftJoin(joinResult, join);
                } else if (join.isRight()) {
                    joinResult = calculateHeuristicRightJoin(joinResult, join);
                } else if (join.isCross()) {
                    joinResult = calculateHeuristicCrossJoin(joinResult, join);
                } else if (join.isFull()) {
                    joinResult = calculateHeuristicFullJoin(joinResult, join);
                } else {
                    throw new IllegalArgumentException("Join type not supported: " + join);
                }
            }
            return joinResult;
        }

        throw new IllegalArgumentException();
    }

    private SqlHeuristicResult calculateHeuristicFullJoin(SqlHeuristicResult leftRowSetResult, Join fullJoin) {
        if (!fullJoin.isFull()) {
            throw new IllegalArgumentException("Join is not a FULL JOIN");
        }

        final FromItem rightFromItem = fullJoin.getRightItem();
        final Collection<Expression> onExpressions = fullJoin.getOnExpressions();

        SqlHeuristicResult rightRowSetResult = calculateHeuristicRowSet(rightFromItem);

        QueryResult leftQueryResult = leftRowSetResult.getQueryResult();
        QueryResult rightQueryResult = rightRowSetResult.getQueryResult();

        QueryResult fullJoinQueryResult = createFullJoin(leftQueryResult, rightQueryResult, onExpressions);

        Truthness truthness = TruthnessUtils.buildOrAggregationTruthness(leftRowSetResult.getTruthness(),
                rightRowSetResult.getTruthness());

        return new SqlHeuristicResult(truthness, fullJoinQueryResult);
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

    private QueryResult createRightJoin(QueryResult leftQueryResult, QueryResult
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

    private QueryResult createFullJoin(QueryResult leftQueryResult,
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


    private QueryResult createLeftJoin(QueryResult leftQueryResult, QueryResult
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


    SqlHeuristicResult calculateHeuristic(Select select) {
        tableColumnResolver.enterStatementeContext(select);
        final SqlHeuristicResult heuristicResult;
        if (select instanceof SetOperationList) {
            SetOperationList unionQuery = (SetOperationList) select;
            List<Select> subqueries = unionQuery.getSelects();
            heuristicResult = calculateHeuristicUnion(subqueries);
        } else if (select instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            Select subquery = parenthesedSelect.getSelect();
            heuristicResult = calculateHeuristic(subquery);
        } else if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            final FromItem fromItem = getFrom(plainSelect);
            final List<Join> joins = getJoins(plainSelect);
            final Expression whereClause = getWhere(plainSelect);

            final SqlHeuristicResult intermediateHeuristicResult = calculateHeuristic(fromItem, joins, whereClause);
            QueryResult queryResult = buildQueryResultUsingSelectItems(plainSelect.getSelectItems(), intermediateHeuristicResult.getQueryResult());
            heuristicResult = new SqlHeuristicResult(intermediateHeuristicResult.getTruthness(), queryResult);
        } else {
            throw new IllegalArgumentException("Cannot calculate heuristics for SQL command of type " + select.getClass().getName());
        }
        tableColumnResolver.exitCurrentStatementContext();
        return heuristicResult;
    }

    private SqlHeuristicResult calculateHeuristic(FromItem fromItem, List<Join> joins, Expression whereClause) {
        SqlHeuristicResult rowSetResult = calculateHeuristicRowSet(fromItem, joins);
        QueryResult rowSet = rowSetResult.getQueryResult();
        final SqlHeuristicResult unfilteredQueryResult;
        if (whereClause == null) {
            /**
             * Handle case of SELECT/FROM without WHERE clause
             */
            unfilteredQueryResult = rowSetResult;
        } else {
            /**
             * Handle caseof SELECT/FROM with WHERE clause
             */
            SqlHeuristicResult conditionResult = calculateHeuristicCondition(whereClause, rowSet);
            Truthness truthness = TruthnessUtils.buildAndAggregationTruthness(rowSetResult.getTruthness(), conditionResult.getTruthness());
            unfilteredQueryResult = new SqlHeuristicResult(truthness, conditionResult.getQueryResult());
        }
        return unfilteredQueryResult;
    }

    private QueryResult buildQueryResultUsingSelectItems(List<SelectItem<?>> selectItems, QueryResult queryResult) {
        if (selectItems == null || selectItems.isEmpty()) {
            return queryResult;
        }

        final List<VariableDescriptor> filteredVariableDescriptors = buildVariableDescriptorsUsingSelectItems(selectItems, queryResult);

        QueryResult filteredQueryResult = new QueryResult(filteredVariableDescriptors);
        if (queryResult.isEmpty() && !hasAnyTableColumn(selectItems)) {
            final List<Object> filteredValues = buildConcreteValuesUsingSelectItems(selectItems);
            DataRow singleRow = new DataRow(filteredVariableDescriptors, filteredValues);
            filteredQueryResult.addRow(singleRow);
        } else {
            for (DataRow row : queryResult.seeRows()) {
                final List<Object> filteredValues = buildConcreteValuesUsingSelectItems(selectItems, row);
                DataRow filteredRow = new DataRow(filteredVariableDescriptors, filteredValues);
                filteredQueryResult.addRow(filteredRow);
            }
        }
        return filteredQueryResult;
    }

    private List<Object> buildConcreteValuesUsingSelectItems(List<SelectItem<?>> selectItems, DataRow row) {
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

    private List<Object> buildConcreteValuesUsingSelectItems(List<SelectItem<?>> selectItems) {
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

    private List<VariableDescriptor> buildVariableDescriptorsUsingSelectItems(List<SelectItem<?>> selectItems, QueryResult queryResult) {
        List<VariableDescriptor> variableDescriptors = new ArrayList<>();

        for (SelectItem<?> selectItem : selectItems) {
            if (selectItem.getExpression() instanceof AllTableColumns) {
                AllTableColumns allTableColumns = (AllTableColumns) selectItem.getExpression();
                String tableName = allTableColumns.getTable().getName();
                for (VariableDescriptor variableDescriptor : queryResult.seeVariableDescriptors()) {
                    if (tableName.equalsIgnoreCase(variableDescriptor.getAliasTableName())
                            || tableName.equalsIgnoreCase(variableDescriptor.getTableName())) {
                        variableDescriptors.add(variableDescriptor);
                    }
                }
            } else if (selectItem.getExpression() instanceof AllColumns) {
                variableDescriptors.addAll(queryResult.seeVariableDescriptors());
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
                variableDescriptors.add(variableDescriptor);
            } else {
                // create unnamed column
                String columnName = selectItem.getAlias() != null
                        ? selectItem.getAlias().getName()
                        : null;
                VariableDescriptor variableDescriptor = new VariableDescriptor(columnName);
                variableDescriptors.add(variableDescriptor);
            }
        }
        return variableDescriptors;
    }

    private SqlHeuristicResult calculateHeuristicUnion(List<Select> subqueries) {
        List<SqlHeuristicResult> subqueryResults = new ArrayList<>();
        for (Select subquery : subqueries) {
            SqlHeuristicResult subqueryResult = calculateHeuristic(subquery);
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

    private QueryResult getQueryResultForFromItem(FromItem fromItem) {
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
