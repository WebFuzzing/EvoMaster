package org.evomaster.client.java.controller.internal.db;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.evomaster.client.java.controller.internal.db.constraint.CheckExprExtractor;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SQLParserTest {

    private static ComparisonExpr lte(CheckExpr left, CheckExpr right) {
        return new ComparisonExpr(left, ComparisonOperator.LESS_THAN_OR_EQUAL, right);
    }

    private static ComparisonExpr eq(CheckExpr left, CheckExpr right) {
        return new ComparisonExpr(left, ComparisonOperator.EQUALS_TO, right);
    }

    private static ColumnName column(String columnName) {
        return new ColumnName(columnName);
    }

    private static AndFormula and(CheckExpr left, CheckExpr right) {
        return new AndFormula(left, right);
    }

    private static BigIntegerLiteral intL(int i) {
        return new BigIntegerLiteral(i);
    }

    private static StringLiteral str(String literalValue) {
        return new StringLiteral(literalValue);
    }

    private static CheckExpr isNotNull(ColumnName columnName) {
        return new IsNotNullExpr(columnName);
    }


    private static InExpression in(String columName, String... stringLiteralValues) {
        List<CheckExpr> stringLiterals = Arrays.stream(stringLiteralValues).map(SQLParserTest::str).collect(Collectors.toList());
        CheckExprList stringLiteralList = new CheckExprList(stringLiterals);
        return new InExpression(column(columName), stringLiteralList);
    }


    @Test
    void testMinorThanOrEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max<=10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("age_max".toUpperCase()), ComparisonOperator.LESS_THAN_OR_EQUAL, new BigIntegerLiteral(10));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testMinorThan() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max<10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("age_max".toUpperCase()), ComparisonOperator.LESS_THAN, new BigIntegerLiteral(10));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testGreaterThanValue() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max>10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("age_max".toUpperCase()), ComparisonOperator.GREATER_THAN, new BigIntegerLiteral(10));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testMinorThanColumn() throws SqlParseException {
        SqlParser parser = SqlParser.create("10<age_max".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new BigIntegerLiteral(10), ComparisonOperator.LESS_THAN, new ColumnName("age_max".toUpperCase()));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testMinorThanValue() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max<10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("age_max".toUpperCase()), ComparisonOperator.LESS_THAN, new BigIntegerLiteral(10));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testGreaterThanEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max>=10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("age_max".toUpperCase()), ComparisonOperator.GREATER_THAN_OR_EQUAL, new BigIntegerLiteral(10));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testMinorThanEqualsColumn() throws SqlParseException {
        SqlParser parser = SqlParser.create("10<=age_max".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new BigIntegerLiteral(10), ComparisonOperator.LESS_THAN_OR_EQUAL, new ColumnName("age_max".toUpperCase()));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max=10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("age_max".toUpperCase()), ComparisonOperator.EQUALS_TO, new BigIntegerLiteral(10));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testTableAndColumnName() throws SqlParseException {
        SqlParser parser = SqlParser.create("users.age_max<=100".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new ColumnName("users".toUpperCase(), "age_max".toUpperCase()), ComparisonOperator.LESS_THAN_OR_EQUAL, new BigIntegerLiteral(100));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testAndExpression() throws SqlParseException {
        SqlParser parser = SqlParser.create("18<=age_max AND age_max<=100".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);

        CheckExpr expectedCheckExpr = and(
                lte(intL(18), column("age_max".toUpperCase())),
                lte(column("age_max".toUpperCase()), intL(100)));

        assertEquals(expectedCheckExpr, checkExpr);

    }

    @Test
    void testParenthesis() throws SqlParseException {
        SqlParser parser = SqlParser.create("18<=age_max".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = new ComparisonExpr(new BigIntegerLiteral(18), ComparisonOperator.LESS_THAN_OR_EQUAL, new ColumnName("age_max".toUpperCase()));
        assertEquals(expectedCheckExpr, checkExpr);
    }

    @Test
    void testInCondition() throws SqlParseException {
        SqlParser parser = SqlParser.create("(status in ('a', 'b'))");
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr checkExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedCheckExpr = in("status".toUpperCase(), "a", "b");
        assertEquals(expectedCheckExpr, checkExpr);

    }

    @Test
    void testConditionEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("(status = 'b') = (p_at IS NOT NULL)");
        SqlNode sqlExpr = parser.parseExpression();
        CheckExprExtractor sqlExtractor = new CheckExprExtractor();
        CheckExpr actualExpr = sqlExpr.accept(sqlExtractor);
        CheckExpr expectedExpr = eq(
                eq(column("status".toUpperCase()), str("b")),
                isNotNull(column("p_at".toUpperCase())));
        assertEquals(expectedExpr, actualExpr);
    }

    @Test
    void testLike() throws SqlParseException {
        SqlParser parser = SqlParser.create("(f_id LIKE 'hi'\n" +
                "    OR f_id LIKE '%foo%'\n" +
                "    OR f_id LIKE '%foo%x%'\n" +
                "    OR f_id LIKE '%bar%'\n" +
                "    OR f_id LIKE '%bar%y%'\n" +
                "    OR f_id LIKE '%hello%')");
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testMultipleInCondition() throws SqlParseException {
        SqlParser parser = SqlParser.create("(status IN ('A', 'B', 'C', 'D', 'E'))");
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testSimilarTo() throws SqlParseException {
        SqlParser parser = SqlParser.create("(w_id SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?')");
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testIsNull() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max IS NULL".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testIsNotNull() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max IS NOT NULL".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testEqualsNull() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max = NULL".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testBrokenExpression() {
        SqlParser parser = SqlParser.create("age_max === NULL".toUpperCase());
        try {
            parser.parseExpression();
            fail();
        } catch (SqlParseException e) {

        }
    }

}
