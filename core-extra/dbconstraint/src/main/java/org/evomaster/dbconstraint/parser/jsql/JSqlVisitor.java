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

public class JSqlVisitor extends ExpressionVisitorAdapter<Void> {

    private static final String SIMILAR_TO = "similar_to";
    private static final String SIMILAR_ESCAPE = "similar_escape";
    private static final String SIMILAR_TO_ESCAPE = "similar_to_escape";

    private final Deque<SqlCondition> stack = new ArrayDeque<>();

    @Override
    public <S> Void visit(BitwiseRightShift bitwiseRightShift, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(BitwiseLeftShift bitwiseLeftShift, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(NullValue nullValue, S data) {
        stack.push(new SqlNullLiteralValue());
        return null;
    }

    @Override
    public <S> Void visit(Function function, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(SignedExpression signedExpression, S data) {
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
        return null;
    }

    @Override
    public <S> Void visit(JdbcParameter jdbcParameter, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(JdbcNamedParameter jdbcNamedParameter, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(DoubleValue doubleValue, S data) {
        stack.push(new SqlBigDecimalLiteralValue(doubleValue.getValue()));
        return null;
    }

    @Override
    public <S> Void visit(LongValue longValue, S data) {
        stack.push(new SqlBigIntegerLiteralValue(longValue.getBigIntegerValue()));
        return null;
    }

    @Override
    public <S> Void visit(HexValue hexValue, S data) {
        stack.push(new SqlBinaryDataLiteralValue(hexValue.getValue()));
        return null;
    }

    @Override
    public <S> Void visit(DateValue dateValue, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TimeValue timeValue, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TimestampValue timestampValue, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(StringValue stringValue, S data) {
        String notEscapedValue = stringValue.getNotExcapedValue();

        String notEscapedValueNoQuotes;
        if (notEscapedValue.startsWith("'") && notEscapedValue.endsWith("'")) {
            notEscapedValueNoQuotes = notEscapedValue.substring(1, notEscapedValue.length() - 1);
        } else {
            notEscapedValueNoQuotes = notEscapedValue;
        }
        stack.push(new SqlStringLiteralValue(notEscapedValueNoQuotes));
        return null;
    }

    @Override
    public <S> Void visit(Addition addition, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Division division, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(IntegerDivision division, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Multiplication multiplication, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Subtraction subtraction, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(AndExpression andExpression, S data) {
        andExpression.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        andExpression.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlAndCondition(left, right));
        return null;
    }

    @Override
    public <S> Void visit(OrExpression orExpression, S data) {
        orExpression.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        orExpression.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlOrCondition(left, right));
        return null;
    }

    @Override
    public <S> Void visit(XorExpression orExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Between between, S data) {
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
        return null;
    }

    @Override
    public <S> Void visit(OverlapsCondition overlapsCondition, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(EqualsTo equalsTo, S data) {
        equalsTo.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        equalsTo.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.EQUALS_TO, right));
        return null;
    }


    @Override
    public <S> Void visit(GreaterThan greaterThan, S data) {
        greaterThan.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        greaterThan.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.GREATER_THAN, right));
        return null;
    }

    @Override
    public <S> Void visit(GreaterThanEquals greaterThanEquals, S data) {
        greaterThanEquals.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        greaterThanEquals.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.GREATER_THAN_OR_EQUAL, right));
        return null;
    }

    @Override
    public <S> Void visit(InExpression inExpression, S data) {
        inExpression.getLeftExpression().accept(this);
        SqlColumn left = (SqlColumn) stack.pop();
        inExpression.getRightExpression().accept(this);
        SqlConditionList right = (SqlConditionList) stack.pop();
        stack.push(new SqlInCondition(left, right));
        return null;
    }

    @Override
    public <S> Void visit(FullTextSearch fullTextSearch, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(IsNullExpression isNullExpression, S data) {
        isNullExpression.getLeftExpression().accept(this);
        SqlColumn columnName = (SqlColumn) stack.pop();
        if (isNullExpression.isNot()) {
            stack.push(new SqlIsNotNullCondition(columnName));
        } else {
            stack.push(new SqlIsNullCondition(columnName));
        }
        return null;
    }

    @Override
    public <S> Void visit(IsBooleanExpression isBooleanExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(LikeExpression likeExpression, S data) {
        likeExpression.getLeftExpression().accept(this);
        SqlColumn left = (SqlColumn) stack.pop();
        likeExpression.getRightExpression().accept(this);
        SqlStringLiteralValue pattern = (SqlStringLiteralValue) stack.pop();
        stack.push(new SqlLikeCondition(left, pattern));
        return null;
    }

    @Override
    public <S> Void visit(MinorThan minorThan, S data) {
        minorThan.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        minorThan.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN, right));
        return null;
    }

    @Override
    public <S> Void visit(MinorThanEquals minorThanEquals, S data) {
        minorThanEquals.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        minorThanEquals.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN_OR_EQUAL, right));
        return null;
    }

    @Override
    public <S> Void visit(NotEqualsTo notEqualsTo, S data) {
        notEqualsTo.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        notEqualsTo.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.NOT_EQUALS_TO, right));
        return null;
    }

    @Override
    public <S> Void visit(DoubleAnd doubleAnd, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Contains contains, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ContainedBy containedBy, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ParenthesedSelect parenthesedSelect, S data) {
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
    public <S> Void visit(Column column, S data) {
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
        return null;
    }



    @Override
    public <S> Void visit(ExpressionList<? extends Expression> expressionList, S data) {
        List<SqlCondition> sqlConditionList = new ArrayList<>();
        for (Expression expr : expressionList) {
            expr.accept(this);
            SqlCondition sqlCondition = stack.pop();
            sqlConditionList.add(sqlCondition);
        }
        stack.push(new SqlConditionList(sqlConditionList));
        return null;
    }


    @Override
    public <S> Void visit(CaseExpression caseExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(WhenClause whenClause, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ExistsExpression existsExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(MemberOfExpression memberOfExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(AnyComparisonExpression anyComparisonExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Concat concat, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Matches matches, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(BitwiseAnd bitwiseAnd, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(BitwiseOr bitwiseOr, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(BitwiseXor bitwiseXor, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    /**
     * e.g. 'hi'::text, 'hi' AS VARCHAR
     *
     * @param castExpression the casting expression
     */
    @Override
    public <S> Void visit(CastExpression castExpression, S data) {
        castExpression.getLeftExpression().accept(this);
        return null;
    }

    @Override
    public <S> Void visit(Modulo modulo, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(AnalyticExpression analyticExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ExtractExpression extractExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(IntervalExpression intervalExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(OracleHierarchicalExpression oracleHierarchicalExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(RegExpMatchOperator regExpMatchOperator, S data) {

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
        return null;
    }

    @Override
    public <S> Void visit(JsonExpression jsonExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(JsonOperator jsonOperator, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }



    @Override
    public <S> Void visit(UserVariable userVariable, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(NumericBind numericBind, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(KeepExpression keepExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(MySQLGroupConcat mySQLGroupConcat, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }



    @Override
    public <S> Void visit(RowConstructor<? extends Expression> rowConstructor, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(RowGetExpression rowGetExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(OracleHint oracleHint, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TimeKeyExpression timeKeyExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(DateTimeLiteralExpression dateTimeLiteralExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(NotExpression notExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(NextValExpression nextValExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(CollateExpression collateExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(SimilarToExpression aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ArrayExpression aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ArrayConstructor aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(VariableAssignment aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(XMLSerializeExpr aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TimezoneExpression aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(JsonAggregateFunction aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(JsonFunction aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(ConnectByRootOperator aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(OracleNamedFunctionParameter aThis, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(AllColumns allColumns, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(AllTableColumns allTableColumns, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(AllValue allValue, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(IsDistinctExpression isDistinctExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(GeometryDistance geometryDistance, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(Select select, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TranscodingFunction transcodingFunction, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TrimFunction trimFunction, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(RangeExpression rangeExpression, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TSQLLeftJoin tsqlLeftJoin, S data) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public <S> Void visit(TSQLRightJoin tsqlRightJoin, S data) {
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
