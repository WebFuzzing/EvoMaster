package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.*;
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JSqlConditionParserTest {

    @Test
    public void test0() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("c >= 100", ConstraintDatabaseType.H2);
    }
    @Test
    public void testIn() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("(status IN ('A', 'B'))", ConstraintDatabaseType.H2);
    }

    @Test
    public void testInText() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition condition = parser.parse("(status IN ('A'::text, 'B'::text))", ConstraintDatabaseType.H2);
        assertTrue(condition instanceof SqlInCondition);
    }

    @Test
    public void testParseAnyTwoOptions() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text))", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status = ANY (ARRAY['A'::text, 'B'::text]))", ConstraintDatabaseType.H2);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseAnyMultipleOptions() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text, 'C'::text, 'D'::text, 'E'::text))", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status = ANY (ARRAY['A'::text, 'B'::text, 'C'::text, 'D'::text, 'E'::text]))", ConstraintDatabaseType.H2);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseAnyNoSpaces() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text))", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status=ANY(ARRAY['A'::text, 'B'::text]))", ConstraintDatabaseType.H2);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseAnyManySpaces() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text))", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status   =   ANY    (     ARRAY    ['A'::text, 'B'::text]   )    )", ConstraintDatabaseType.H2);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseEquals() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("(status = 'B'::text)", ConstraintDatabaseType.H2);
    }

    @Test
    public void testParseIsNotNull() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("(p_at IS NOT NULL)", ConstraintDatabaseType.H2);
    }

    @Test
    public void testParseEqualFormulas() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("((status = 'B'::text) = (p_at IS NOT NULL))", ConstraintDatabaseType.H2);
    }


    @Test
    public void testParseEnumManySpaces() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A', 'B'))", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status enum ('A', 'B'))", ConstraintDatabaseType.MYSQL);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseCastAsVarchar() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ( 'hi', 'low'  ) )", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status IN ( CAST('hi' AS VARCHAR(2)), CAST('low' AS VARCHAR(3) ) ) )", ConstraintDatabaseType.H2);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseCastAsCharacterLargeObject() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ( CAST('hi' AS VARCHAR(2)), CAST('low' AS VARCHAR(3) ) ) )", ConstraintDatabaseType.H2);
        SqlCondition actual = parser.parse("(status IN ( CAST('hi' AS CHARACTER LARGE OBJECT(2)), CAST('low' AS CHARACTER LARGE OBJECT(3) ) ) )", ConstraintDatabaseType.H2);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseLowerFunction() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        // LOWER(col) should be treated as col (case-folding dropped as approximation)
        SqlCondition withLower = parser.parse("LOWER(commit_mgr_desc) != 'init'", ConstraintDatabaseType.H2);
        SqlCondition withoutLower = parser.parse("commit_mgr_desc != 'init'", ConstraintDatabaseType.H2);
        assertEquals(withoutLower, withLower);
    }

    @Test
    public void testParseUpperFunction() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition withUpper = parser.parse("UPPER(status) != 'ACTIVE'", ConstraintDatabaseType.H2);
        SqlCondition withoutUpper = parser.parse("status != 'ACTIVE'", ConstraintDatabaseType.H2);
        assertEquals(withoutUpper, withUpper);
    }

    @Test
    public void testParseIsNotNullOrLower() throws SqlConditionParserException {
        // Pattern seen in tracking-system: col IS NOT NULL OR LOWER(other_col) != 'init'
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition condition = parser.parse(
                "commit_emp_desc IS NOT NULL OR LOWER(commit_mgr_desc) != 'init'",
                ConstraintDatabaseType.H2);
        assertInstanceOf(SqlOrCondition.class, condition);
    }

    @Test
    public void testParseTimestampLiteral() throws SqlConditionParserException {
        // Pattern seen in tracking-system: col = TIMESTAMP 'datetime-string'
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition condition = parser.parse(
                "commit_date = TIMESTAMP '2020-11-26 10:49:41'",
                ConstraintDatabaseType.H2);
        assertInstanceOf(SqlComparisonCondition.class, condition);
        SqlComparisonCondition cmp = (SqlComparisonCondition) condition;
        assertInstanceOf(SqlBigIntegerLiteralValue.class, cmp.getRightOperand());
        // The epoch value must be computed in UTC so that the round-trip in
        // SMTLibZ3DbConstraintSolver (LocalDateTime.ofInstant(..., UTC)) preserves
        // the original string representation regardless of JVM timezone.
        SqlBigIntegerLiteralValue epoch = (SqlBigIntegerLiteralValue) cmp.getRightOperand();
        long expected = java.time.LocalDateTime.parse("2020-11-26 10:49:41",
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .toEpochSecond(java.time.ZoneOffset.UTC);
        assertEquals(java.math.BigInteger.valueOf(expected), epoch.getBigInteger());
    }

}
