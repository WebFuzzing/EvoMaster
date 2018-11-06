package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLParserTest {

    @Test
    public void testMinorThanEquals() throws JSQLParserException {
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
    public void testMinorThan() throws JSQLParserException {
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
    public void testGreaterThanValue() throws JSQLParserException {
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
    public void testMinorThanColumn() throws JSQLParserException {
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
    public void testMinorThanValue() throws JSQLParserException {
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
    public void testGreaterThanEquals() throws JSQLParserException {
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
    public void testMinorThanEqualsColumn() throws JSQLParserException {
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
    public void testEquals() throws JSQLParserException {
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
    public void testTableAndColumnName() throws JSQLParserException {
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
    public void testAndExpression() throws JSQLParserException {
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
    public void testParenthesis() throws JSQLParserException {
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
}
