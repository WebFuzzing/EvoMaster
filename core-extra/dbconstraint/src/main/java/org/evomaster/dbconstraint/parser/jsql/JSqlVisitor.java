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

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class JSqlVisitor implements ExpressionVisitor {

    private static final String SIMILAR_TO = "similar_to";
    private static final String SIMILAR_ESCAPE = "similar_escape";
    private static final String SIMILAR_TO_ESCAPE = "similar_to_escape";

    private static final String LOWER = "LOWER";
    private static final String UPPER = "UPPER";

    private static final String ANY = "ANY";
    private static final String SOME = "SOME";

    private static final String SINGLE_QUOTE_CHAR = "'";

    /**
     * Accepts the timestamp layouts a database or an ORM actually emits, rather than a single one.
     *
     * The date is mandatory; everything after it is optional. The time may be separated by a space
     * or by the ISO 'T', may omit the seconds, and may carry a fractional part of any precision. A
     * trailing UTC offset is honoured in either the "+HH:mm"/"Z" or the "+HH" spelling.
     *
     * Widening this matters more than it looks: when a literal cannot be read, the resulting
     * exception makes the caller drop the *whole* WHERE clause, including the conditions it could
     * otherwise have translated. An ORM emits sub-second precision whenever it compares a column
     * against the current instant, so the narrow form lost entire clauses routinely.
     *
     * The fractional part is parsed but not used: the result is expressed in whole epoch seconds,
     * matching the decoder in SMTLibZ3DbConstraintSolver.
     */
    /**
     * The time of day, with everything after the hour and minute optional. Built separately so it can
     * be attached to each separator as a unit: were the separator and the time independently
     * optional, a literal consisting of a date and a stray separator would satisfy the pattern and be
     * silently read as midnight.
     */
    private static final DateTimeFormatter TIME_OF_DAY = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
            .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

    private static DateTimeFormatter timeAfter(char separator) {
        return new DateTimeFormatterBuilder()
                .appendLiteral(separator)
                .append(TIME_OF_DAY)
                .toFormatter();
    }

    private static final DateTimeFormatter TIMESTAMP_PARSER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            // Either separator, each carrying the whole time of day with it, so that neither can
            // appear without one. A date on its own remains valid; a date with a dangling separator,
            // or with an offset but no time, does not.
            .appendOptional(timeAfter('T'))
            .appendOptional(timeAfter(' '))
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

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
        String name = function.getName().toUpperCase();
        if ((name.equals(LOWER) || name.equals(UPPER))
                && function.getParameters() != null
                && function.getParameters().size() == 1) {
            // Treat LOWER(col)/UPPER(col) as the column itself (case-folding is dropped as an approximation)
            function.getParameters().get(0).accept(this);
            return;
        }
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
        // Treat the timestamp string as UTC so the epoch round-trips consistently with the
        // UTC-based decoder in SMTLibZ3DbConstraintSolver (LocalDateTime.ofInstant(..., UTC)).
        long epochSeconds = timestampValue.getValue().toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
        stack.push(new SqlBigIntegerLiteralValue(BigInteger.valueOf(epochSeconds)));
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(StringValue stringValue) {
        String notEscapedValue = stringValue.getNotExcapedValue();

        String notEscapedValueNoQuotes;
        if (notEscapedValue.startsWith(SINGLE_QUOTE_CHAR) && notEscapedValue.endsWith(SINGLE_QUOTE_CHAR)) {
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

        ArrayConstructor alternatives = membershipArrayOf(equalsTo.getRightExpression());
        if (alternatives != null) {
            /*
             * "col = ANY (ARRAY['A','B'])" is a membership test, equivalent to
             * "col IN ('A','B')", so it is translated to the node that already means that.
             */
            if (!(left instanceof SqlColumn)) {
                throw new RuntimeException("Extraction of condition not yet implemented");
            }
            alternatives.accept(this);
            SqlConditionList literals = (SqlConditionList) stack.pop();
            stack.push(new SqlInCondition((SqlColumn) left, literals));
            return;
        }

        equalsTo.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        if (right instanceof SqlConditionList) {
            /*
             * A bare "col = ARRAY[...]" compares a value against an array rather than testing
             * membership in it, and there is no node for that. Reading it as an IN would answer a
             * different question than the constraint asks, so it is left untranslated.
             */
            throw new RuntimeException("Extraction of condition not yet implemented");
        }
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.EQUALS_TO, right));
    }

    /**
     * The array of alternatives in a membership test, or null if the expression is not one.
     *
     * <p>Only {@code ANY} and {@code SOME} qualify, and only over an array literal. {@code ALL}
     * requires every element to match rather than one, and {@code ANY} over a subquery carries no
     * literals to enumerate, so neither can be answered with an {@code IN}.
     */
    private static ArrayConstructor membershipArrayOf(Expression expression) {
        if (!(expression instanceof Function)) {
            return null;
        }
        Function function = (Function) expression;
        String name = function.getName().toUpperCase();
        if (!name.equals(ANY) && !name.equals(SOME)) {
            return null;
        }
        ExpressionList<?> parameters = function.getParameters();
        if (parameters == null || parameters.size() != 1
                || !(parameters.get(0) instanceof ArrayConstructor)) {
            return null;
        }
        return (ArrayConstructor) parameters.get(0);
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
        SqlConditionList right = unwrapSingletonArray((SqlConditionList) stack.pop());
        stack.push(new SqlInCondition(left, right));
    }

    /**
     * Flattens the one nested level that {@code IN (ARRAY[...])} produces.
     *
     * <p>The parenthesis around the array is itself an expression list, so the array's elements
     * arrive wrapped in a list of one. The alternatives are the elements, not the array.
     */
    private static SqlConditionList unwrapSingletonArray(SqlConditionList list) {
        List<SqlCondition> elements = list.getSqlConditionExpressions();
        if (elements.size() == 1 && elements.get(0) instanceof SqlConditionList) {
            return (SqlConditionList) elements.get(0);
        }
        return list;
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
        notEqualsTo.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        notEqualsTo.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.NOT_EQUALS_TO, right));
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
        if (dateTimeLiteralExpression.getType() == DateTimeLiteralExpression.DateTime.TIMESTAMP) {
            String value = dateTimeLiteralExpression.getValue();
            if (value.startsWith(SINGLE_QUOTE_CHAR) && value.endsWith(SINGLE_QUOTE_CHAR)) {
                value = value.substring(1, value.length() - 1);
            }
            TemporalAccessor parsed = TIMESTAMP_PARSER.parse(value);
            // An explicit offset is honoured; without one, treat the literal as UTC to match the
            // UTC-based decoder in SMTLibZ3DbConstraintSolver (LocalDateTime.ofInstant(..., UTC)).
            long epochSeconds = parsed.isSupported(ChronoField.OFFSET_SECONDS)
                    ? OffsetDateTime.from(parsed).toEpochSecond()
                    : LocalDateTime.from(parsed).toEpochSecond(ZoneOffset.UTC);
            stack.push(new SqlBigIntegerLiteralValue(BigInteger.valueOf(epochSeconds)));
            return;
        }
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

    /**
     * An array literal, e.g. {@code ARRAY['A','B']}, becomes the list of its elements: the same
     * {@link SqlConditionList} that {@link #visit(ExpressionList)} builds for the right-hand side of
     * an {@code IN}. What that list means is decided by whoever consumes it, since an array is a
     * value in its own right and only membership tests turn it into a set of alternatives.
     */
    @Override
    public void visit(ArrayConstructor aThis) {
        aThis.getExpressions().accept(this);
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
