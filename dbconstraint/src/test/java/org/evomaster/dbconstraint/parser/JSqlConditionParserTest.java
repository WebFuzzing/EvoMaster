package org.evomaster.dbconstraint.parser;

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
        parser.parse("c >= 100");
    }

    @Test
    public void testIn() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("(status IN ('A', 'B'))");
    }

    @Test
    public void testInText() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition condition = parser.parse("(status IN ('A'::text, 'B'::text))");
        assertTrue(condition instanceof SqlInCondition);
    }

    @Test
    public void testParseAnyTwoOptions() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text))");
        SqlCondition actual = parser.parse("(status = ANY (ARRAY['A'::text, 'B'::text]))");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseAnyMultipleOptions() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text, 'C'::text, 'D'::text, 'E'::text))");
        SqlCondition actual = parser.parse("(status = ANY (ARRAY['A'::text, 'B'::text, 'C'::text, 'D'::text, 'E'::text]))");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseAnyNoSpaces() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text))");
        SqlCondition actual = parser.parse("(status=ANY(ARRAY['A'::text, 'B'::text]))");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseAnyManySpaces() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        SqlCondition expected = parser.parse("(status IN ('A'::text, 'B'::text))");
        SqlCondition actual = parser.parse("(status   =   ANY    (     ARRAY    ['A'::text, 'B'::text]   )    )");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseEquals() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("(status = 'B'::text)");
    }

    @Test
    public void testParseIsNotNull() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("(p_at IS NOT NULL)");
    }

    @Test
    public void testParseEqualFormulas() throws SqlConditionParserException {
        JSqlConditionParser parser = new JSqlConditionParser();
        parser.parse("((status = 'B'::text) = (p_at IS NOT NULL))");
    }
}
