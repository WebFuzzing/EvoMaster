package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.evomaster.client.java.controller.internal.db.constraint.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SQLParserTest {

    @Test
    void testMinorThanEquals() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("age_max<=10");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof UpperBoundConstraint);
        UpperBoundConstraint upperBound = (UpperBoundConstraint) constraint;
        assertEquals("age_max", upperBound.getColumnName());
        assertEquals(10, upperBound.getUpperBound());
    }

    @Test
    void testMinorThan() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("age_max<10");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof UpperBoundConstraint);
        UpperBoundConstraint upperBound = (UpperBoundConstraint) constraint;
        assertEquals("age_max", upperBound.getColumnName());
        assertEquals(9, upperBound.getUpperBound());
    }

    @Test
    void testGreaterThanValue() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("age_max>10");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof LowerBoundConstraint);
        LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint;
        assertEquals("age_max", lowerBound.getColumnName());
        assertEquals(11, lowerBound.getLowerBound());
    }

    @Test
    void testMinorThanColumn() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("10<age_max");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof LowerBoundConstraint);
        LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint;
        assertEquals("age_max", lowerBound.getColumnName());
        assertEquals(11, lowerBound.getLowerBound());
    }


    @Test
    void testMinorThanValue() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("age_max<10");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof UpperBoundConstraint);
        UpperBoundConstraint upperBound = (UpperBoundConstraint) constraint;
        assertEquals("age_max", upperBound.getColumnName());
        assertEquals(9, upperBound.getUpperBound());
    }


    @Test
    void testGreaterThanEquals() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("age_max>=10");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof LowerBoundConstraint);
        LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint;
        assertEquals("age_max", lowerBound.getColumnName());
        assertEquals(10, lowerBound.getLowerBound());
    }

    @Test
    void testMinorThanEqualsColumn() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("10<=age_max");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof LowerBoundConstraint);
        LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint;
        assertEquals("age_max", lowerBound.getColumnName());
        assertEquals(10, lowerBound.getLowerBound());
    }


    @Test
    void testEquals() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("age_max=10");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof RangeConstraint);
        RangeConstraint rangeConstraint = (RangeConstraint) constraint;
        assertEquals("age_max", rangeConstraint.getColumnName());
        assertEquals(10, rangeConstraint.getMinValue());
        assertEquals(10, rangeConstraint.getMaxValue());
    }

    @Test
    void testTableAndColumnName() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("users.age_max<=100");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof UpperBoundConstraint);
        UpperBoundConstraint upperBound = (UpperBoundConstraint) constraint;
        assertEquals("users", upperBound.getTableName());
        assertEquals("age_max", upperBound.getColumnName());
        assertEquals(100, upperBound.getUpperBound());
    }

    @Test
    void testAndExpression() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("18<=age_max AND age_max<=100");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(2, constraints.size());
        SchemaConstraint constraint0 = constraints.get(0);
        assertTrue(constraint0 instanceof LowerBoundConstraint);
        LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint0;
        assertEquals("age_max", lowerBound.getColumnName());
        assertEquals(18, lowerBound.getLowerBound());

        SchemaConstraint constraint1 = constraints.get(1);
        assertTrue(constraint1 instanceof UpperBoundConstraint);
        UpperBoundConstraint upperBound = (UpperBoundConstraint) constraint1;
        assertEquals("age_max", upperBound.getColumnName());
        assertEquals(100, upperBound.getUpperBound());

    }

    @Test
    void testParenthesis() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("(18<=age_max)");
        CheckExprExtractor extractor = new CheckExprExtractor();
        expr.accept(extractor);
        List<SchemaConstraint> constraints = extractor.getConstraints();
        assertEquals(1, constraints.size());
        SchemaConstraint constraint0 = constraints.get(0);
        assertTrue(constraint0 instanceof LowerBoundConstraint);
        LowerBoundConstraint lowerBound = (LowerBoundConstraint) constraint0;
        assertEquals("age_max", lowerBound.getColumnName());
        assertEquals(18, lowerBound.getLowerBound());
    }

    @Test
    void testInCondition() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("(status in ('A', 'B'))");
        assertNotNull(expr);
    }

    @Test
    void testConditionEquals() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("(status = 'B') = (p_at IS NOT NULL)");
        assertNotNull(expr);
    }

    @Test
    void testLike() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("(f_id LIKE 'hi'\n" +
                "    OR f_id LIKE '%foo%'\n" +
                "    OR f_id LIKE '%foo%x%'\n" +
                "    OR f_id LIKE '%bar%'\n" +
                "    OR f_id LIKE '%bar%y%'\n" +
                "    OR f_id LIKE '%hello%')");
        assertNotNull(expr);
    }

    @Test
    void testMultipleInCondition() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseCondExpression("(status IN ('A', 'B', 'C', 'D', 'E'))");
        assertNotNull(expr);
    }


    /**
     * Shows that the SIMILAR TO construct is not supported by this SQL Parser
     */
    @Test
    void testSimilarNotSupported() {
        try {
            CCJSqlParserUtil.parseCondExpression("(w_id SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?')");
            fail();
        } catch (JSQLParserException e) {
            // do nothing
        }
    }


}
