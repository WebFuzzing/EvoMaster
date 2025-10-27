package org.evomaster.dbconstraint.parser.jsql;

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
import org.evomaster.dbconstraint.ast.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class JSqlVisitor implements ExpressionVisitor {

    private static final String SIMILAR_TO = "similar_to";
    private static final String SIMILAR_ESCAPE = "similar_escape";
    private static final String SIMILAR_TO_ESCAPE = "similar_to_escape";

    private final Deque<SqlCondition> stack = new ArrayDeque<>();

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NullValue nullValue) {
        stack.push(new SqlNullLiteralValue());
    }

    @Override
    public void visit(Function function) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        signedExpression.getExpression().accept(this);
        SqlCondition sqlCondition = stack.pop();
        if (sqlCondition instanceof SqlLiteralValue) {
            SqlLiteralValue sqlLiteralValue = (SqlLiteralValue) sqlCondition;
            SqlLiteralValue negated;
            if (sqlLiteralValue instanceof SqlBigIntegerLiteralValue) {
                SqlBigIntegerLiteralValue sqlBigIntegerLiteralValue = (SqlBigIntegerLiteralValue) sqlLiteralValue;
                negated = new SqlBigIntegerLiteralValue(sqlBigIntegerLiteralValue.getBigInteger().negate());
            } else if (sqlLiteralValue instanceof SqlBigDecimalLiteralValue) {
                SqlBigDecimalLiteralValue sqlBigDecimalLiteralValue = (SqlBigDecimalLiteralValue) sqlLiteralValue;
                negated = new SqlBigDecimalLiteralValue(sqlBigDecimalLiteralValue.getBigDecimal().negate());

            } else {
                throw new RuntimeException("Extraction of condition not yet implemented for literal value class " + sqlLiteralValue.getClass());
            }
            stack.push(negated);
        } else {
            throw new RuntimeException("Extraction of condition not yet implemented for " + sqlCondition.getClass());
        }
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        stack.push(new SqlBigDecimalLiteralValue(doubleValue.getValue()));
    }

    @Override
    public void visit(LongValue longValue) {
        stack.push(new SqlBigIntegerLiteralValue(longValue.getBigIntegerValue()));
    }

    @Override
    public void visit(HexValue hexValue) {
        stack.push(new SqlBinaryDataLiteralValue(hexValue.getValue()));
    }

    @Override
    public void visit(DateValue dateValue) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimeValue timeValue) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(StringValue stringValue) {
        String notEscapedValue = stringValue.getNotExcapedValue();

        String notEscapedValueNoQuotes;
        if (notEscapedValue.startsWith("'") && notEscapedValue.endsWith("'")) {
            notEscapedValueNoQuotes = notEscapedValue.substring(1, notEscapedValue.length() - 1);
        } else {
            notEscapedValueNoQuotes = notEscapedValue;
        }
        stack.push(new SqlStringLiteralValue(notEscapedValueNoQuotes));
    }

    @Override
    public void visit(Addition addition) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Division division) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(IntegerDivision division) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Multiplication multiplication) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Subtraction subtraction) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AndExpression andExpression) {
        andExpression.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        andExpression.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlAndCondition(left, right));
    }

    @Override
    public void visit(OrExpression orExpression) {
        orExpression.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        orExpression.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlOrCondition(left, right));
    }

    @Override
    public void visit(XorExpression orExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Between between) {
        between.getLeftExpression().accept(this);
        SqlCondition leftExpression = stack.pop();

        between.getBetweenExpressionStart().accept(this);
        SqlCondition startExpression = stack.pop();

        between.getBetweenExpressionEnd().accept(this);
        SqlCondition endExpression = stack.pop();

        SqlCondition leftCondition = new SqlComparisonCondition(
                leftExpression,
                SqlComparisonOperator.GREATER_THAN_OR_EQUAL,
                startExpression
        );

        SqlCondition rightCondition = new SqlComparisonCondition(
                leftExpression,
                SqlComparisonOperator.LESS_THAN_OR_EQUAL,
                endExpression
        );

        stack.push(new SqlAndCondition(leftCondition, rightCondition));
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        equalsTo.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        equalsTo.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.EQUALS_TO, right));
    }


    @Override
    public void visit(GreaterThan greaterThan) {
        greaterThan.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        greaterThan.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.GREATER_THAN, right));
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        greaterThanEquals.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        greaterThanEquals.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.GREATER_THAN_OR_EQUAL, right));
    }

    @Override
    public void visit(InExpression inExpression) {
        inExpression.getLeftExpression().accept(this);
        SqlColumn left = (SqlColumn) stack.pop();
        inExpression.getRightExpression().accept(this);
        SqlConditionList right = (SqlConditionList) stack.pop();
        stack.push(new SqlInCondition(left, right));
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        isNullExpression.getLeftExpression().accept(this);
        SqlColumn columnName = (SqlColumn) stack.pop();
        if (isNullExpression.isNot()) {
            stack.push(new SqlIsNotNullCondition(columnName));
        } else {
            stack.push(new SqlIsNullCondition(columnName));
        }
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        likeExpression.getLeftExpression().accept(this);
        SqlColumn left = (SqlColumn) stack.pop();
        likeExpression.getRightExpression().accept(this);
        SqlStringLiteralValue pattern = (SqlStringLiteralValue) stack.pop();
        stack.push(new SqlLikeCondition(left, pattern));
    }

    @Override
    public void visit(MinorThan minorThan) {
        minorThan.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        minorThan.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN, right));
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        minorThanEquals.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        minorThanEquals.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN_OR_EQUAL, right));
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(DoubleAnd doubleAnd) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Contains contains) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ContainedBy containedBy) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ParenthesedSelect parenthesedSelect) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    private static final String QUOTE_CHAR = "\"";

    private static boolean hasSurroundingQuotes(String str) {
        return str.length()>1 && str.startsWith(QUOTE_CHAR) && str.endsWith(QUOTE_CHAR);
    }

    private static String removeSurroundingQuotes(String str) {
        return str.substring(1,str.length()-1);
    }

    @Override
    public void visit(Column column) {
        String columnName = column.getColumnName();

        /*
         * The SQL:1999 standard specifies that double quote (")
         * (QUOTATION MARK) is used to delimit identifiers.
         * Oracle, PostgreSQL, MySQL, MSSQL and SQlite all
         * support " as the identifier delimiter.
         * e.g.
         * 'foo' is an SQL string
         * "foo" is an SQL identifier (column/table/etc)
         *
         * https://stackoverflow.com/questions/2901453/sql-standard-to-escape-column-names
         */
        if (hasSurroundingQuotes(columnName)) {
            columnName = removeSurroundingQuotes(columnName);
        }
        if (column.getTable() != null) {
            String tableName = column.getTable().getName();
            stack.push(new SqlColumn(tableName, columnName));
        } else {
            stack.push(new SqlColumn(columnName));
        }
    }



    @Override
    public void visit(ExpressionList<?> expressionList) {
        List<SqlCondition> sqlConditionList = new ArrayList<>();
        for (Expression expr : expressionList) {
            expr.accept(this);
            SqlCondition sqlCondition = stack.pop();
            sqlConditionList.add(sqlCondition);
        }
        stack.push(new SqlConditionList(sqlConditionList));
    }






    @Override
    public void visit(CaseExpression caseExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(WhenClause whenClause) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(MemberOfExpression memberOfExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Concat concat) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Matches matches) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    /**
     * e.g. 'hi'::text, 'hi' AS VARCHAR
     *
     * @param castExpression the casting expression
     */
    @Override
    public void visit(CastExpression castExpression) {
        castExpression.getLeftExpression().accept(this);
    }

    @Override
    public void visit(Modulo modulo) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AnalyticExpression analyticExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ExtractExpression extractExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(IntervalExpression intervalExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(RegExpMatchOperator regExpMatchOperator) {

        regExpMatchOperator.getLeftExpression().accept(this);
        SqlColumn columnName = (SqlColumn) this.stack.pop();

        String operator1 = regExpMatchOperator.getStringExpression();
        if (!operator1.equals("~")) {
            throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
        }

        if (regExpMatchOperator.getRightExpression() instanceof SignedExpression) {
            SignedExpression signedRightExpression = (SignedExpression) regExpMatchOperator.getRightExpression();
            String operator2 = String.valueOf(signedRightExpression.getSign());
            if (!operator2.equals("~")) {
                throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
            }
            signedRightExpression.getExpression().accept(this);
            SqlStringLiteralValue pattern = (SqlStringLiteralValue) this.stack.pop();

            stack.push(new SqlLikeCondition(columnName, pattern));

        } else if (regExpMatchOperator.getRightExpression() instanceof Function) {
            Function function = (Function) regExpMatchOperator.getRightExpression();
            if (!function.getName().equals(SIMILAR_TO) && !function.getName().equals(SIMILAR_ESCAPE) && !function.getName().equals(SIMILAR_TO_ESCAPE)) {
                throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
            }
            function.getParameters().accept(this);
            SqlConditionList parameterList = (SqlConditionList) stack.pop();
            SqlStringLiteralValue pattern = (SqlStringLiteralValue) parameterList.getSqlConditionExpressions().get(0);
            stack.push(new SqlSimilarToCondition(columnName, pattern));

        } else {
            throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
        }

    }

    @Override
    public void visit(JsonExpression jsonExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JsonOperator jsonOperator) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }



    @Override
    public void visit(UserVariable userVariable) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NumericBind numericBind) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(KeepExpression keepExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(MySQLGroupConcat mySQLGroupConcat) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }



    @Override
    public void visit(RowConstructor rowConstructor) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(OracleHint oracleHint) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NotExpression notExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NextValExpression nextValExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(SimilarToExpression aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ArrayExpression aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ArrayConstructor aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(VariableAssignment aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(XMLSerializeExpr aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimezoneExpression aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JsonAggregateFunction aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JsonFunction aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ConnectByRootOperator aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(OracleNamedFunctionParameter aThis) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AllColumns allColumns) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AllValue allValue) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Select select) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TranscodingFunction transcodingFunction) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TrimFunction trimFunction) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TSQLLeftJoin tsqlLeftJoin) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TSQLRightJoin tsqlRightJoin) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    /**
     * Return the constraints collected during the visit to the AST
     *
     * @return the current sql condition
     */
    public SqlCondition getSqlCondition() {
        return this.stack.peek();
    }
}
