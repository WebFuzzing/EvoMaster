package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext;
import org.evomaster.client.java.sql.advanced.helpers.truthness.ObjectTruthnessHelper;
import org.evomaster.client.java.sql.advanced.query_calculator.CalculationResult;
import org.evomaster.client.java.sql.advanced.query_calculator.QueryCalculator;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.expressions.ListExpression;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.expressions.SelectComparisonExpression;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.expressions.SelectExpression;
import org.evomaster.client.java.sql.advanced.query_contextualizer.SubQueryContextualizer;
import org.evomaster.client.java.sql.advanced.select_query.SelectQuery;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.Truthness.TRUE;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext.createEvaluationContext;
import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToDate;
import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToDouble;
import static org.evomaster.client.java.sql.advanced.helpers.LiteralsHelper.isBooleanLiteral;
import static org.evomaster.client.java.sql.advanced.helpers.LiteralsHelper.isTrueLiteral;
import static org.evomaster.client.java.sql.advanced.query_calculator.QueryCalculator.createQueryCalculator;
import static org.evomaster.client.java.sql.advanced.query_contextualizer.SubQueryContextualizer.createSubQueryContextualizer;
import static org.evomaster.client.java.sql.advanced.select_query.QueryColumn.createQueryColumn;

public class WhereCalculator implements ExpressionVisitor {

    public static final char MINUS_SIGN = '-';
    public static final String QUOTE = "'";
    public static final String EMPTY_STRING = "";

    private Expression where;
    private EvaluationContext evaluationContext;
    private WhereCalculatorStack stack;
    private SqlDriver sqlDriver;

    private WhereCalculator(Expression where, EvaluationContext evaluationContext, SqlDriver sqlDriver) {
        this.where = where;
        this.evaluationContext = evaluationContext;
        this.stack = new WhereCalculatorStack();
        this.sqlDriver = sqlDriver;
    }

    public static WhereCalculator createWhereCalculator(SelectQuery query, Row row, SqlDriver sqlDriver) {
        EvaluationContext evaluationContext = createEvaluationContext(query.getFromTables(), row);
        return new WhereCalculator(query.getWhere(), evaluationContext, sqlDriver);
    }

    public Truthness calculate() {
        where.accept(this);
        Truthness truthness = stack.popTruthness();
        SimpleLogger.debug(format("Truthness between between WHERE: %s and row: %s was %s", where, evaluationContext, truthness));
        return truthness;
    }

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(NullValue nullValue) {
        stack.pushExpression(null);
    }

    @Override
    public void visit(Function function) {
        stack.pushExpression(evaluationContext.getValue(createQueryColumn(function.toString())));
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        signedExpression.getExpression().accept(this);
        Double numberWithoutSign = convertToDouble(stack.popNumber());
        Double number = signedExpression.getSign() != MINUS_SIGN ? numberWithoutSign : -numberWithoutSign;
        stack.pushNumber(number);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        stack.pushNumber(doubleValue.getValue());
    }

    @Override
    public void visit(LongValue longValue) {
        stack.pushNumber(longValue.getValue());
    }

    @Override
    public void visit(HexValue hexValue) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(DateValue dateValue) {
        stack.pushExpression(dateValue.getValue());
    }

    @Override
    public void visit(TimeValue timeValue) {
        stack.pushExpression(timeValue.getValue());
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        stack.pushExpression(timestampValue.getValue());
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(StringValue stringValue) {
        stack.pushExpression(stringValue.getValue());
    }

    @Override
    public void visit(Addition addition) {
        addition.getRightExpression().accept(this);
        addition.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) + convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(Division division) {
        division.getRightExpression().accept(this);
        division.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) / convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(IntegerDivision integerDivision) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Multiplication multiplication) {
        multiplication.getRightExpression().accept(this);
        multiplication.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) * convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(Subtraction subtraction) {
        subtraction.getRightExpression().accept(this);
        subtraction.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) - convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(AndExpression andExpression) {
        andExpression.getRightExpression().accept(this);
        andExpression.getLeftExpression().accept(this);
        stack.pushTruthness(andAggregation(stack.popTruthness(), stack.popTruthness()));
    }

    @Override
    public void visit(OrExpression orExpression) {
        orExpression.getRightExpression().accept(this);
        orExpression.getLeftExpression().accept(this);
        stack.pushTruthness(orAggregation(stack.popTruthness(), stack.popTruthness()));
    }

    @Override
    public void visit(XorExpression xorExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Between between) {
        between.getLeftExpression().accept(this);
        between.getBetweenExpressionStart().accept(this);
        between.getBetweenExpressionEnd().accept(this);

        Object maxValue = stack.popExpression();
        Object minValue = stack.popExpression();
        Object value = stack.popExpression();

        if(!between.isNot()) {
            Truthness truthnessMin = ObjectTruthnessHelper.calculateTruthnessForGreaterThanOrEquals(value, minValue);
            Truthness truthnessMax = ObjectTruthnessHelper.calculateTruthnessForMinorThanOrEquals(value, maxValue);
            stack.pushTruthness(andAggregation(truthnessMin, truthnessMax));
        } else {
            Truthness truthnessMin = ObjectTruthnessHelper.calculateTruthnessForMinorThan(value, minValue);
            Truthness truthnessMax = ObjectTruthnessHelper.calculateTruthnessForGreaterThan(value, maxValue);
            stack.pushTruthness(orAggregation(truthnessMin, truthnessMax));
        }
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        calculateTruthnessForComparisonOperator(equalsTo);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        calculateTruthnessForComparisonOperator(greaterThan);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        calculateTruthnessForComparisonOperator(greaterThanEquals);
    }

    private void calculateTruthnessForComparisonOperator(ComparisonOperator operator) {
        operator.getRightExpression().accept(this);
        operator.getLeftExpression().accept(this);

        Object leftValue = stack.popExpression();
        Object rightValue = stack.popExpression();

        if(rightValue instanceof SelectComparisonExpression) {
            SelectComparisonExpression selectComparisonExpression = (SelectComparisonExpression) rightValue;
            switch (selectComparisonExpression.getAnyType()) {
                case ALL:
                    stack.pushTruthness(
                        calculateTruthnessForSubquery(selectComparisonExpression.getSelect(), calculationResult ->
                            all(leftValue, rowValues(calculationResult.getRows()), getTruthnessFunction(operator))));
                    break;
                case ANY:
                case SOME:
                default:
                    stack.pushTruthness(
                        calculateTruthnessForSubquery(selectComparisonExpression.getSelect(), calculationResult ->
                            any(leftValue, rowValues(calculationResult.getRows()), getTruthnessFunction(operator))));
            }
        } else {
            stack.pushTruthness(getTruthnessFunction(operator).apply(leftValue, rightValue));
        }
    }

    private static Truthness any(Object testValue, List<Object> values, BiFunction<Object, Object, Truthness> truthnessFunction){
        if(!values.isEmpty()) {
            Double maxRowTruthness = values.stream()
                .map(value -> truthnessFunction.apply(testValue, value))
                .map(Truthness::getOfTrue)
                .max(Double::compareTo)
                .orElseThrow(() -> new RuntimeException("Values must not be empty"));
            return trueOrScaleTrue(maxRowTruthness);
        } else {
            return FALSE;
        }
    }

    private static Truthness all(Object testValue, List<Object> values, BiFunction<Object, Object, Truthness> truthnessFunction){
        if(!values.isEmpty()) {
            Double minRowTruthness = values.stream()
                .map(value -> truthnessFunction.apply(testValue, value))
                .map(Truthness::getOfTrue)
                .min(Double::compareTo)
                .orElseThrow(() -> new RuntimeException("Values must not be empty"));
            return trueOrScaleTrue(minRowTruthness);
        } else {
            return TRUE;
        }
    }

    private static BiFunction<Object, Object, Truthness> getTruthnessFunction(ComparisonOperator operator) {
        if(operator instanceof EqualsTo) {
            return ObjectTruthnessHelper::calculateTruthnessForEquals;
        } else if(operator instanceof NotEqualsTo) {
            return ObjectTruthnessHelper::calculateTruthnessForNotEquals;
        } else if(operator instanceof GreaterThan) {
            return ObjectTruthnessHelper::calculateTruthnessForGreaterThan;
        } else if(operator instanceof GreaterThanEquals) {
            return ObjectTruthnessHelper::calculateTruthnessForGreaterThanOrEquals;
        } else if(operator instanceof MinorThan) {
            return ObjectTruthnessHelper::calculateTruthnessForMinorThan;
        } else if(operator instanceof MinorThanEquals) {
            return ObjectTruthnessHelper::calculateTruthnessForMinorThanOrEquals;
        } else {
            throw new UnsupportedOperationException("This comparison operator is not supported yet");
        }
    }

    @Override
    public void visit(InExpression inExpression) {
        inExpression.getRightExpression().accept(this);
        inExpression.getLeftExpression().accept(this);

        Object testValue = stack.popExpression();
        Object valuesExpression = stack.popExpression();

        if(valuesExpression instanceof ListExpression) {
            List<Object> values = ((ListExpression) valuesExpression).getValues();
            if(!inExpression.isNot()) {
                stack.pushTruthness(any(testValue, values, ObjectTruthnessHelper::calculateTruthnessForEquals));
            } else {
                stack.pushTruthness(all(testValue, values, ObjectTruthnessHelper::calculateTruthnessForNotEquals));
            }
        } else if(valuesExpression instanceof SelectExpression) {
            Select select = ((SelectExpression) valuesExpression).getSelect();
            if(!inExpression.isNot()) {
                stack.pushTruthness(
                    calculateTruthnessForSubquery(select, calculationResult ->
                        any(testValue, rowValues(calculationResult.getRows()),
                            ObjectTruthnessHelper::calculateTruthnessForEquals)));
            } else {
                stack.pushTruthness(
                    calculateTruthnessForSubquery(select, calculationResult ->
                        all(testValue, rowValues(calculationResult.getRows()),
                            ObjectTruthnessHelper::calculateTruthnessForNotEquals)));
            }
        } else {
            throw new RuntimeException("Values must be a list or a subquery");
        }
    }

    private Truthness calculateTruthnessForSubquery(Select select, java.util.function.Function<CalculationResult, Truthness> truthnessFunction) {
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriver);
        String subQuery = subQueryContextualizer.contextualize();
        QueryCalculator queryCalculator = createQueryCalculator(subQuery, sqlDriver);
        CalculationResult calculationResult = queryCalculator.calculate();
        return truthnessFunction.apply(calculationResult);
    }

    private List<Object> rowValues(List<Row> rows) {
        return rows.stream()
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(Map::values)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        isNullExpression.getLeftExpression().accept(this);
        if(!isNullExpression.isNot()) {
            stack.pushTruthness(trueIfConditionElseFalse(isNull(stack.popExpression())));
        } else {
            stack.pushTruthness(trueIfConditionElseFalse(nonNull(stack.popExpression())));
        }
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        isBooleanExpression.getLeftExpression().accept(this);
        Boolean value = stack.popBoolean();
        if(!isNull(value)) {
            if (!isBooleanExpression.isNot()) {
                if (isBooleanExpression.isTrue()) {
                    stack.pushTruthness(trueIfConditionElseFalse(value));
                } else {
                    stack.pushTruthness(trueIfConditionElseFalse(!value));
                }
            } else {
                if (isBooleanExpression.isTrue()) {
                    stack.pushTruthness(trueIfConditionElseFalse(!value));
                } else {
                    stack.pushTruthness(trueIfConditionElseFalse(value));
                }
            }
        } else {
            stack.pushTruthness(FALSE);
        }
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(MinorThan minorThan) {
        calculateTruthnessForComparisonOperator(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        calculateTruthnessForComparisonOperator(minorThanEquals);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        calculateTruthnessForComparisonOperator(notEqualsTo);
    }

    @Override
    public void visit(DoubleAnd doubleAnd) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Contains contains) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ContainedBy containedBy) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ParenthesedSelect parenthesedSelect) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Column column) {
        if(evaluationContext.includes(createQueryColumn(column))) {
            Object value = evaluationContext.getValue(createQueryColumn(column));
            if(value instanceof Number) {
                stack.pushNumber((Number) value);
            } else {
                stack.pushExpression(value);
            }
        } else if(isBooleanLiteral(column.getColumnName())) {
            stack.pushBoolean(isTrueLiteral(column.getColumnName()));
        } else {
            throw new RuntimeException(format("Column %s must be present in %s or be a boolean literal", column, evaluationContext));
        }
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(WhenClause whenClause) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        Select select = (Select) existsExpression.getRightExpression();
        Truthness truthness = calculateTruthnessForSubquery(select, CalculationResult::getTruthness);
        if(!existsExpression.isNot()) {
            stack.pushTruthness(truthness);
        } else {
            stack.pushTruthness(truthness.invert());
        }
    }

    @Override
    public void visit(MemberOfExpression memberOfExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        stack.pushExpression(
            new SelectComparisonExpression(
                anyComparisonExpression.getSelect(), anyComparisonExpression.getAnyType()));
    }

    @Override
    public void visit(Concat concat) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Matches matches) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(CastExpression castExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Modulo modulo) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(AnalyticExpression analyticExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ExtractExpression extractExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(IntervalExpression intervalExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(RegExpMatchOperator regExpMatchOperator) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(JsonExpression jsonExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(JsonOperator jsonOperator) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(UserVariable userVariable) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(NumericBind numericBind) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(KeepExpression keepExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(MySQLGroupConcat mySQLGroupConcat) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ExpressionList<?> expressionList) {
        expressionList.forEach(expression -> expression.accept(this));
        ListExpression listExpression =
            new ListExpression(Stream.generate(() -> stack.popExpression())
                .limit(expressionList.size())
                .collect(Collectors.toList()));
        stack.pushExpression(listExpression);
    }

    @Override
    public void visit(RowConstructor<?> rowConstructor) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(OracleHint oracleHint) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {
        String unquotedStringLiteral = dateTimeLiteralExpression.getValue().replace(QUOTE, EMPTY_STRING);
        Date date = convertToDate(unquotedStringLiteral);
        stack.pushExpression(date);
    }

    @Override
    public void visit(NotExpression notExpression) {
        notExpression.getExpression().accept(this);
        stack.pushTruthness(stack.popTruthness().invert());
    }

    @Override
    public void visit(NextValExpression nextValExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(SimilarToExpression similarToExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ArrayExpression arrayExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(XMLSerializeExpr xmlSerializeExpr) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(TimezoneExpression timezoneExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(JsonAggregateFunction jsonAggregateFunction) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(JsonFunction jsonFunction) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(ConnectByRootOperator connectByRootOperator) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(AllColumns allColumns) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(AllValue allValue) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(Select select) {
        stack.pushExpression(new SelectExpression(select));
    }

    @Override
    public void visit(TranscodingFunction transcodingFunction) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(TrimFunction trimFunction) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(TSQLLeftJoin tsqlLeftJoin) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }

    @Override
    public void visit(TSQLRightJoin tsqlRightJoin) {
        throw new UnsupportedOperationException("Unsupported SQL feature");
    }
}