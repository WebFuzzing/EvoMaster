package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.internal.db.constraint.SqlConditionParserException;
import org.evomaster.client.java.controller.internal.db.constraint.calcite.CalciteSqlSqlConditionParser;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlConditionParserTest {

    private static SqlComparisonCondition lte(SqlCondition left, SqlCondition right) {
        return new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN_OR_EQUAL, right);
    }

    private static SqlComparisonCondition eq(SqlCondition left, SqlCondition right) {
        return new SqlComparisonCondition(left, SqlComparisonOperator.EQUALS_TO, right);
    }

    private static SqlColumnName column(String columnName) {
        return new SqlColumnName(columnName);
    }

    private static SqlAndCondition and(SqlCondition left, SqlCondition right) {
        return new SqlAndCondition(left, right);
    }

    private static SqlBigIntegerLiteralValue intL(int i) {
        return new SqlBigIntegerLiteralValue(i);
    }

    private static SqlStringLiteralValue str(String literalValue) {
        return new SqlStringLiteralValue(literalValue);
    }

    private static SqlIsNotNullCondition isNotNull(SqlColumnName sqlColumnName) {
        return new SqlIsNotNullCondition(sqlColumnName);
    }


    private static SqlInCondition in(String columName, String... stringLiteralValues) {
        List<SqlCondition> stringLiterals = Arrays.stream(stringLiteralValues).map(SqlConditionParserTest::str).collect(Collectors.toList());
        SqlConditionList stringLiteralList = new SqlConditionList(stringLiterals);
        return new SqlInCondition(column(columName), stringLiteralList);
    }

    private static SqlCondition parse(String conditionSqlStr) throws SqlConditionParserException {
        CalciteSqlSqlConditionParser parser = new CalciteSqlSqlConditionParser();
        return parser.parse(conditionSqlStr);
    }

    private static SqlSimilarToCondition similarTo(SqlColumnName columnName, SqlStringLiteralValue pattern) {
        return new SqlSimilarToCondition(columnName, pattern);
    }

    private static SqlIsNullCondition isNull(SqlColumnName columnName) {
        return new SqlIsNullCondition(columnName);
    }

    private static SqlLikeCondition like(SqlColumnName columnName, String patternStr) {
        return new SqlLikeCondition(columnName, new SqlStringLiteralValue(patternStr));
    }

    private static SqlOrCondition or(SqlCondition... conditions) {
        return new SqlOrCondition(conditions);
    }


    @Test
    void testMinorThanOrEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max<=10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }


    @Test
    void testMinorThan() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max<10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testGreaterThanValue() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max>10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("age_max".toUpperCase()), SqlComparisonOperator.GREATER_THAN, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testMinorThanColumn() throws SqlConditionParserException {
        SqlCondition actual = parse("10<age_max".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlBigIntegerLiteralValue(10), SqlComparisonOperator.LESS_THAN, new SqlColumnName("age_max".toUpperCase()));
        assertEquals(expected, actual);
    }

    @Test
    void testMinorThanValue() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max<10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testGreaterThanEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max>=10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("age_max".toUpperCase()), SqlComparisonOperator.GREATER_THAN_OR_EQUAL, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testMinorThanEqualsColumn() throws SqlConditionParserException {
        SqlCondition actual = parse("10<=age_max".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlBigIntegerLiteralValue(10), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlColumnName("age_max".toUpperCase()));
        assertEquals(expected, actual);
    }

    @Test
    void testEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max=10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("age_max".toUpperCase()), SqlComparisonOperator.EQUALS_TO, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testTableAndColumnName() throws SqlConditionParserException {
        SqlCondition actual = parse("users.age_max<=100".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumnName("users".toUpperCase(), "age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlBigIntegerLiteralValue(100));
        assertEquals(expected, actual);
    }

    @Test
    void testAndExpression() throws SqlConditionParserException {
        SqlCondition actual = parse("18<=age_max AND age_max<=100".toUpperCase());
        SqlCondition expected = and(
                lte(intL(18), column("age_max".toUpperCase())),
                lte(column("age_max".toUpperCase()), intL(100)));

        assertEquals(expected, actual);

    }

    @Test
    void testParenthesis() throws SqlConditionParserException {
        SqlCondition actual = parse("18<=age_max".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlBigIntegerLiteralValue(18), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlColumnName("age_max".toUpperCase()));
        assertEquals(expected, actual);
    }

    @Test
    void testInCondition() throws SqlConditionParserException {
        SqlCondition actual = parse("(status in ('a', 'b'))");
        SqlCondition expected = in("status".toUpperCase(), "a", "b");
        assertEquals(expected, actual);

    }

    @Test
    void testConditionEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("(status = 'b') = (p_at IS NOT NULL)");
        SqlCondition expected = eq(
                eq(column("status".toUpperCase()), str("b")),
                isNotNull(column("p_at".toUpperCase())));
        assertEquals(expected, actual);
    }

    @Test
    void testSingleLike() throws SqlConditionParserException {
        SqlCondition actual = parse("(f_id LIKE 'hi')");
        SqlLikeCondition expected = like(column("F_ID"), "hi");
        assertEquals(expected, actual);
    }


    @Test
    void testMultiLike() throws SqlConditionParserException {
        SqlCondition actual = parse("(f_id LIKE 'hi'\n" +
                "    OR f_id LIKE '%foo%'\n" +
                "    OR f_id LIKE '%foo%x%'\n" +
                "    OR f_id LIKE '%bar%'\n" +
                "    OR f_id LIKE '%bar%y%'\n" +
                "    OR f_id LIKE '%hello%')");
        SqlOrCondition expected = or(or(or(or(or(like(column("F_ID"), "hi"),
                like(column("F_ID"), "%foo%")),
                like(column("F_ID"), "%foo%x%")),
                like(column("F_ID"), "%bar%")),
                like(column("F_ID"), "%bar%y%")),
                like(column("F_ID"), "%hello%")
        );
        assertEquals(expected, actual);
    }

    @Test
    void testMultipleInCondition() throws SqlConditionParserException {
        SqlCondition actual = parse("(status IN ('A', 'B', 'C', 'D', 'E'))");
        SqlInCondition expected = in("STATUS", "A", "B", "C", "D", "E");
        assertEquals(expected, actual);
    }

    @Test
    void testSimilarTo() throws SqlConditionParserException {
        SqlCondition actual = parse("(W_ID SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?')");
        SqlSimilarToCondition expected = similarTo(column("W_ID"), str("/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"));
        assertEquals(expected, actual);
    }


    @Test
    void testIsNull() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max IS NULL".toUpperCase());
        SqlIsNullCondition expected = isNull(column("AGE_MAX"));
        assertEquals(expected, actual);
    }


    @Test
    void testIsNotNull() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max IS NOT NULL".toUpperCase());
        SqlIsNotNullCondition expected = isNotNull(column("AGE_MAX"));
        assertEquals(expected, actual);
    }

    @Test
    void testEqualsNull() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max = NULL".toUpperCase());
        SqlComparisonCondition expected = eq(column("AGE_MAX"), new SqlNullLiteralValue());
        assertEquals(expected, actual);
    }

    @Test
    void testBrokenExpression() {
        try {
            parse("age_max === NULL".toUpperCase());
            fail();
        } catch (SqlConditionParserException e) {

        }
    }

}
