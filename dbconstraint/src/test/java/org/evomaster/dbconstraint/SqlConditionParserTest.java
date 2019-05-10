package org.evomaster.dbconstraint;

import org.evomaster.dbconstraint.ast.*;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SqlConditionParserTest {

    private static SqlComparisonCondition lte(SqlCondition left, SqlCondition right) {
        return new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN_OR_EQUAL, right);
    }

    private static SqlComparisonCondition eq(SqlCondition left, SqlCondition right) {
        return new SqlComparisonCondition(left, SqlComparisonOperator.EQUALS_TO, right);
    }

    private static SqlColumn column(String columnName) {
        return new SqlColumn(columnName);
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

    private static SqlIsNotNullCondition isNotNull(SqlColumn sqlColumn) {
        return new SqlIsNotNullCondition(sqlColumn);
    }


    private static SqlInCondition in(String columName, String... stringLiteralValues) {
        List<SqlCondition> stringLiterals = Arrays.stream(stringLiteralValues).map(SqlConditionParserTest::str).collect(Collectors.toList());
        SqlConditionList stringLiteralList = new SqlConditionList(stringLiterals);
        return new SqlInCondition(column(columName), stringLiteralList);
    }

    private static SqlCondition parse(String conditionSqlStr) throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        return parser.parse(conditionSqlStr);
    }

    private static SqlSimilarToCondition similarTo(SqlColumn columnName, SqlStringLiteralValue pattern) {
        return new SqlSimilarToCondition(columnName, pattern);
    }

    private static SqlIsNullCondition isNull(SqlColumn columnName) {
        return new SqlIsNullCondition(columnName);
    }

    private static SqlLikeCondition like(SqlColumn columnName, String patternStr) {
        return new SqlLikeCondition(columnName, new SqlStringLiteralValue(patternStr));
    }

    private static SqlOrCondition or(SqlCondition... conditions) {
        return new SqlOrCondition(conditions);
    }


    @Test
    void testMinorThanOrEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max<=10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }


    @Test
    void testMinorThan() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max<10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testGreaterThanValue() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max>10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("age_max".toUpperCase()), SqlComparisonOperator.GREATER_THAN, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testMinorThanColumn() throws SqlConditionParserException {
        SqlCondition actual = parse("10<age_max".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlBigIntegerLiteralValue(10), SqlComparisonOperator.LESS_THAN, new SqlColumn("age_max".toUpperCase()));
        assertEquals(expected, actual);
    }

    @Test
    void testMinorThanValue() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max<10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testGreaterThanEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max>=10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("age_max".toUpperCase()), SqlComparisonOperator.GREATER_THAN_OR_EQUAL, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testMinorThanEqualsColumn() throws SqlConditionParserException {
        SqlCondition actual = parse("10<=age_max".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlBigIntegerLiteralValue(10), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlColumn("age_max".toUpperCase()));
        assertEquals(expected, actual);
    }

    @Test
    void testEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("age_max=10".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("age_max".toUpperCase()), SqlComparisonOperator.EQUALS_TO, new SqlBigIntegerLiteralValue(10));
        assertEquals(expected, actual);
    }

    @Test
    void testTableAndColumnName() throws SqlConditionParserException {
        SqlCondition actual = parse("users.age_max<=100".toUpperCase());
        SqlCondition expected = new SqlComparisonCondition(new SqlColumn("users".toUpperCase(), "age_max".toUpperCase()), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlBigIntegerLiteralValue(100));
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
        SqlCondition expected = new SqlComparisonCondition(new SqlBigIntegerLiteralValue(18), SqlComparisonOperator.LESS_THAN_OR_EQUAL, new SqlColumn("age_max".toUpperCase()));
        assertEquals(expected, actual);
    }

    @Test
    void testInCondition() throws SqlConditionParserException {
        SqlCondition actual = parse("(STATUS in ('a', 'b'))");
        SqlCondition expected = in("STATUS", "a", "b");
        assertEquals(expected, actual);

    }

    @Test
    void testSingleLike() throws SqlConditionParserException {
        SqlCondition actual = parse("(F_ID LIKE 'hi')");
        SqlLikeCondition expected = like(column("F_ID"), "hi");
        assertEquals(expected, actual);
    }


    @Test
    void testConditionEquals() throws SqlConditionParserException {
        SqlCondition actual = parse("((STATUS = 'b') = (P_AT IS NOT NULL))");

        SqlCondition expected = eq(
                eq(column("STATUS"), str("b")),
                isNotNull(column("P_AT")));
        assertEquals(expected, actual);
    }

    @Test
    void testMultiLike() throws SqlConditionParserException {
        SqlCondition actual = parse("(F_ID LIKE 'hi'\n" +
                "    OR F_ID LIKE '%foo%'\n" +
                "    OR F_ID LIKE '%foo%x%'\n" +
                "    OR F_ID LIKE '%bar%'\n" +
                "    OR F_ID LIKE '%bar%y%'\n" +
                "    OR F_ID LIKE '%hello%')");
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
        SqlCondition actual = parse("(STATUS IN ('A', 'B', 'C', 'D', 'E'))");
        SqlInCondition expected = in("STATUS", "A", "B", "C", "D", "E");
        assertEquals(expected, actual);
    }

    @Disabled("SIMILAR TO is not directly supported by JSQL parser")
    @Test
    void testSimilarTo() throws SqlConditionParserException {
        SqlCondition actual = parse("(W_ID SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?')");
        SqlSimilarToCondition expected = similarTo(column("W_ID"), str("/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"));
        assertEquals(expected, actual);
    }

    @Test
    void testPostgresSimilarEscape() throws SqlConditionParserException {
        SqlCondition actual = parse("(w_id ~ similar_escape('/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?'::text, NULL::text))");
        SqlSimilarToCondition expected = similarTo(column("w_id"), str("/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"));
        assertEquals(expected, actual);
    }

    @Test
    void testPostgresLike() throws SqlConditionParserException {
        SqlCondition actual = parse("((f_id ~~ 'hi'::text) OR (f_id ~~ '%foo%'::text) OR (f_id ~~ '%foo%x%'::text) OR (f_id ~~ '%bar%'::text) OR (f_id ~~ '%bar%y%'::text) OR (f_id ~~ '%hello%'::text))");
        SqlOrCondition expected = or(or(or(or(or(like(column("f_id"), "hi"),
                like(column("f_id"), "%foo%")),
                like(column("f_id"), "%foo%x%")),
                like(column("f_id"), "%bar%")),
                like(column("f_id"), "%bar%y%")),
                like(column("f_id"), "%hello%")
        );
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
            SqlCondition e = parse("age_max === NULL".toUpperCase());
            fail();
        } catch (SqlConditionParserException e) {

        }
    }

}
