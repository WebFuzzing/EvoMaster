package org.evomaster.client.java.controller.internal.db;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.evomaster.client.java.controller.internal.db.constraint.SqlCheckExprExtractor;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SQLParserTest {

    private static BinaryComparison lte(ConstraintExpr left, ConstraintExpr right) {
        return new BinaryComparison(left, ComparisonOperator.LESS_THAN_OR_EQUAL, right);
    }

    private static ColumnName column(String columnName) {
        return new ColumnName(columnName);
    }

    private static AndFormula and(ConstraintExpr left, ConstraintExpr right) {
        return new AndFormula(left, right);
    }

    private static BigIntegerLiteral intL(int i) {
        return new BigIntegerLiteral(i);
    }

    @Test
    void testMinorThanOrEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max<=10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("age_max".toUpperCase()), ComparisonOperator.LESS_THAN_OR_EQUAL, new BigIntegerLiteral(10));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testMinorThan() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max<10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("age_max".toUpperCase()), ComparisonOperator.LESS_THAN, new BigIntegerLiteral(10));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testGreaterThanValue() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max>10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("age_max".toUpperCase()), ComparisonOperator.GREATER_THAN, new BigIntegerLiteral(10));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testMinorThanColumn() throws SqlParseException {
        SqlParser parser = SqlParser.create("10<age_max".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new BigIntegerLiteral(10), ComparisonOperator.LESS_THAN, new ColumnName("age_max".toUpperCase()));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testMinorThanValue() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max<10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("age_max".toUpperCase()), ComparisonOperator.LESS_THAN, new BigIntegerLiteral(10));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testGreaterThanEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max>=10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("age_max".toUpperCase()), ComparisonOperator.GREATER_THAN_OR_EQUAL, new BigIntegerLiteral(10));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testMinorThanEqualsColumn() throws SqlParseException {
        SqlParser parser = SqlParser.create("10<=age_max".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new BigIntegerLiteral(10), ComparisonOperator.LESS_THAN_OR_EQUAL, new ColumnName("age_max".toUpperCase()));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("age_max=10".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("age_max".toUpperCase()), ComparisonOperator.EQUALS, new BigIntegerLiteral(10));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testTableAndColumnName() throws SqlParseException {
        SqlParser parser = SqlParser.create("users.age_max<=100".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new ColumnName("users".toUpperCase(), "age_max".toUpperCase()), ComparisonOperator.LESS_THAN_OR_EQUAL, new BigIntegerLiteral(100));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testAndExpression() throws SqlParseException {
        SqlParser parser = SqlParser.create("18<=age_max AND age_max<=100".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);

        ConstraintExpr expectedConstraintExpr = and(
                lte(intL(18), column("age_max".toUpperCase())),
                lte(column("age_max".toUpperCase()), intL(100)));

        assertEquals(expectedConstraintExpr, constraintExpr);

    }

    @Test
    void testParenthesis() throws SqlParseException {
        SqlParser parser = SqlParser.create("18<=age_max".toUpperCase());
        SqlNode sqlExpr = parser.parseExpression();
        SqlCheckExprExtractor sqlExtractor = new SqlCheckExprExtractor();
        ConstraintExpr constraintExpr = sqlExpr.accept(sqlExtractor);
        ConstraintExpr expectedConstraintExpr = new BinaryComparison(new BigIntegerLiteral(18), ComparisonOperator.LESS_THAN_OR_EQUAL, new ColumnName("age_max".toUpperCase()));
        assertEquals(expectedConstraintExpr, constraintExpr);
    }

    @Test
    void testInCondition() throws SqlParseException {
        SqlParser parser = SqlParser.create("(status in ('A', 'B'))");
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }

    @Test
    void testConditionEquals() throws SqlParseException {
        SqlParser parser = SqlParser.create("(status = 'B') = (p_at IS NOT NULL)");
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
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


    /**
     * Shows that the SIMILAR TO construct is not supported by this SQL Parser
     */
    @Test
    void testSimilarNotSupported() throws SqlParseException {
        SqlParser parser = SqlParser.create("(w_id SIMILAR TO '/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?')");
        SqlNode sqlExpr = parser.parseExpression();
        assertNotNull(sqlExpr);
    }


}
