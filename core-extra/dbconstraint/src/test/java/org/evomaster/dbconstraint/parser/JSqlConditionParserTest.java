package org.evomaster.dbconstraint.parser;

import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.ast.SqlInCondition;
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
