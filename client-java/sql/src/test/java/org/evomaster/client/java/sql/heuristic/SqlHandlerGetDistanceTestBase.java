package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.SqlCommandWithDistance;
import org.evomaster.client.java.sql.internal.SqlHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class SqlHandlerGetDistanceTestBase {

    protected abstract Connection getConnection();

    protected abstract DatabaseType getDbType();

    protected abstract void clearDatabase() throws SQLException;

    private DbInfoDto schema;

    @BeforeEach
    public void createAllTables() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE customers\n" +
                "(\n" +
                "    id   INT         NOT NULL,\n" +
                "    name VARCHAR(25) NOT NULL,\n" +
                "    age  INT         NULL,\n" +
                "    CONSTRAINT customers_pk PRIMARY KEY (id)\n" +
                ");");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE orders\n" +
                "(\n" +
                "    id          INT         NOT NULL,\n" +
                "    customer_id INT         NOT NULL,\n" +
                "    product     VARCHAR(25) NOT NULL,\n" +
                "    CONSTRAINT orders_pk PRIMARY KEY (id),\n" +
                "    CONSTRAINT orders_customers_id_fk FOREIGN KEY (customer_id) REFERENCES customers (id)\n" +
                ");");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE employees\n" +
                "(\n" +
                "    id          INT         NOT NULL,\n" +
                "    name        VARCHAR(25) NOT NULL,\n" +
                "    married     TINYINT     NULL,\n" +
                "    hired       DATETIME    NULL,\n" +
                "    CONSTRAINT employees_pk PRIMARY KEY (id)\n" +
                ");");
        schema = DbInfoExtractor.extract(getConnection());
    }

    @AfterEach
    public void dropAllTables() throws Exception {
        clearDatabase();
    }

    private static double rowSet(int length) {
        return TruthnessUtils.getTruthnessToEmpty(length).invert().getOfTrue();
    }

    private double computeSqlDistance(String sqlCommand) {
        final SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setCompleteSqlHeuristics(true);
        sqlHandler.setSchema(schema);
        sqlHandler.setConnection(getConnection());
        sqlHandler.handle(sqlExecutionLogDto);
        List<SqlCommandWithDistance> sqlDistances = sqlHandler.getSqlDistances(null, true);
        assertEquals(1, sqlDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlDistances.iterator().next();
        final double sqlDistance = sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance;
        return sqlDistance;
    }


    @Test
    public void testQueryWithResults() throws Exception { //Query returns results
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 50)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age > 25");
        assertEquals(0, sqlDistance);
    }

    @Test
    public void testQueryWithoutResultsWithoutWhere() { //Query doesn't return results and has not where clause
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers");
        assertEquals(1 - rowSet(0), sqlDistance);
    }

    @Test
    public void testQueryWithoutResultsWithWhere() throws SQLException { //Query doesn't return results, but due to where clause
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = 24");
        // TODO Check expected distance
    }

    @Test
    public void testNumberDouble() throws SQLException { //Double
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = 24.0");
        // TODO Check expected distance
    }

    @Test
    public void testNumberSignedExpressions() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = +24 AND age=-(-24)");
        // TODO Check expected distance
    }

    @Test
    public void testString() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh'");
        // TODO Check expected distance
    }

    @Test
    public void testAndOperator() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age = 25");
        // TODO Check expected distance
    }

    @Test
    public void testOrOperator() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' OR age = 24");
        // TODO Check expected distance
    }

    @Test
    public void testArithmeticOperationsAndParenthesis() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = ((24 - 1 + 1) * 1) / 1");
        // TODO Check expected distance
    }

    @Test
    public void testBooleanOperationWithLiterals() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE true AND false");
        // TODO Check expected distance
    }

    @Test
    public void testBooleanOperationWithNumbers() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE 1 AND 0");
        // TODO Check expected distance
    }

    @Test
    public void testBooleanOperationWithBooleanColumn() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married");
        // TODO Check expected distance
    }

    @Test
    public void testBooleanColumnWithTrue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE name = 'joh' AND married");
        // TODO Check expected distance
    }

    @Test
    public void testBooleanComparisonWithLiteral() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married = false");
        // TODO Check expected distance
    }

    @Test
    public void testBooleanComparisonWithNumber() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married = 0");
        // TODO Check expected distance
    }


    @Test
    public void testIsTrue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married IS TRUE");
        // TODO Check expected distance
    }

    @Test
    public void testIsTrueEvaluatedToTrue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE name = 'joh' AND  married IS TRUE");
        // TODO Check expected distance
    }

    @Test
    public void testIsNotTrue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married IS NOT TRUE");
        // TODO Check expected distance
    }

    @Test
    public void testIsNotTrueEvaluatedToTrue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE name = 'joh' AND married IS NOT TRUE");
        // TODO Check expected distance
    }

    @Test
    public void testIsFalse() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married IS FALSE");
        // TODO Check expected distance
    }

    @Test
    public void testIsFalseWithAnotherCondition() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE name = 'joh' AND married IS FALSE");
        // TODO Check expected distance
    }

    @Test
    public void testIsNotFalse() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', false, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married IS NOT FALSE");
        // TODO Check expected distance
    }

    @Test
    public void testIsNotFalse2() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', true, null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE name = 'joh' AND married IS NOT FALSE");
        // TODO Check expected distance
    }

    @Test
    public void testNot() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 50)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE NOT age > 25");
        // TODO Check expected distance
    }

    @Test
    public void testExistsSubqueryWithResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE EXISTS (SELECT customer_id FROM orders WHERE product = 'guita')");
        // TODO Check expected distance
    }

    @Test
    public void testExistsSubqueryWithoutResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE EXISTS (SELECT customer_id FROM orders WHERE customer_id = customers.id)");
        // TODO Check expected distance
    }

    @Test
    public void testNotExists() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE NOT EXISTS (SELECT customer_id FROM orders WHERE product = 'guita') AND name='joh'");
        // TODO Check expected distance
    }

    @Test
    public void testInListOfValues() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age IN (25, 1)");
        // TODO Check expected distance
    }

    @Test
    public void testInSubqueryWithResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age IN (SELECT 25 UNION SELECT 1)");
        // TODO Check expected distance
    }

    @Test
    public void testInSubqueryWithoutResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE id IN (SELECT customer_id FROM orders)");
        // TODO Check expected distance
    }

    @Test
    public void testNotInListOfValues() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age NOT IN (24, 100)");
        // TODO Check expected distance
    }

    @Test
    public void testNotInSubqueryWithResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age NOT IN (SELECT 24 UNION SELECT 1)");
        // TODO Check expected distance
    }

    @Test
    public void testNotInSubqueryWithoutResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE id NOT IN (SELECT customer_id FROM orders) AND name = 'joh'");
        // TODO Check expected distance
    }

    @Test
    public void testAnySubqueryWithResultsSatisfied() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age = ANY(SELECT 24)");
        // TODO Check expected distance
    }

    @Test
    public void testAnySubqueryWithResultsUnsatisfied() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = ANY(SELECT 25 UNION SELECT 1)");
        // TODO Check expected distance
    }

    @Test
    public void testAnySubqueryWithoutResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE id = ANY(SELECT customer_id FROM orders)");
        // TODO Check expected distance
    }

    @Test
    public void testAllSubqueryWithResultsSatisfied() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age = ALL(SELECT 24)");
        // TODO Check expected distance
    }

    @Test
    public void testAllSubqueryWithResultsUnsatisfied() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = ALL(SELECT 25 UNION SELECT 1)");
        // TODO Check expected distance
    }

    @Test
    public void testAllSubqueryWithoutResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE id = ALL(SELECT customer_id FROM orders) and name = 'joh'");
        // TODO Check expected distance
    }

    @Test
    public void testIsNull() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age IS NULL");
        // TODO Check expected distance
    }

    @Test
    public void testIsNotNull() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 25)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age IS NOT NULL");
        // TODO Check expected distance
    }

    @Test
    public void testIsNotNullValueIsNotNull() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', null)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age IS NOT NULL");
        // TODO Check expected distance
    }

    @Test
    public void testBetweenInRange() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age BETWEEN 22 AND 25");
        // TODO Check expected distance
    }

    @Test
    public void testBetweenNotInRange() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age BETWEEN 26 AND 30");
        // TODO Check expected distance
    }

    @Test
    public void testNotInBetweenNotInRange() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE name = 'joh' AND age NOT BETWEEN 26 AND 30");
        // TODO Check expected distance
    }

    @Test
    public void testNotInBetweenInRange() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age NOT BETWEEN 22 AND 25");
        // TODO Check expected distance
    }

    @Test
    public void testEquals() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 19)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = 20");
        // TODO Check expected distance
    }

    @Test
    public void testNotEquals() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 19)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age != 19");
        // TODO Check expected distance
    }

    @Test
    public void testGreaterThanOrEquals() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 19)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age >= 20");
        // TODO Check expected distance
    }

    @Test
    public void testGreaterThan() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 19)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age > 19");
        // TODO Check expected distance
    }

    @Test
    public void testLessThanOrEquals() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 19)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age <= 18");
        // TODO Check expected distance
    }

    @Test
    public void testLessThan() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 19)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age < 19");
        // TODO Check expected distance
    }

    @Test
    public void testAlias() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers c WHERE " +
                "name = 'joh' AND EXISTS (SELECT customer_id FROM orders WHERE customer_id = c.id)");
        // TODO Check expected distance
    }

    @Test
    public void testAliasUsingAsKeyword() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers AS c " +
                "WHERE name = 'joh' AND EXISTS (SELECT customer_id FROM orders WHERE customer_id = c.id)");
        // TODO Check expected distance
    }

    @Test
    public void testAliasUsingTableNameInsteadOfAlias() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers " +
                "WHERE name = 'joh' AND EXISTS (SELECT customer_id FROM orders WHERE customer_id = customers.id)");
        // TODO Check expected distance
    }

    @Test
    public void testFunctionUpper() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE UPPER(name) = 'JOH'");
        // TODO Check expected distance
    }

    @Test
    public void testFunctionInSubqueryUsingOuterColumn() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john lennon', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'george harrison', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 2, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE " +
                "EXISTS (SELECT customer_id FROM orders WHERE UPPER(name) = 'GEORGE HARRISO')");
        // TODO Check expected distance
    }

    @Test
    public void testGroupBy() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("SELECT COUNT(*) FROM customers WHERE name = 'joh' GROUP BY age");
        // TODO Check expected distance
    }

    @Test
    public void testDateWithDateKeyword() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:01')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE hired = DATE '2024-01-01'");
        // TODO Check expected distance
    }

    @Test
    public void testDateWithDKeyword() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:01')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE hired = {d '2024-01-01'}");
        // TODO Check expected distance
    }

    @Test
    public void testDateWithStringLiteral() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:01')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE hired = '2024-01-01 00:00:00'");
        // TODO Check expected distance
    }

    @Test
    public void testTimestampWithTimestampKeyword() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE hired = TIMESTAMP '2024-01-01 00:00:00.1'");
        // TODO Check expected distance
    }

    @Test
    public void testTimestampWithTsFormat() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE hired = {ts '2024-01-01 00:00:00.1'}");
        // TODO Check expected distance
    }

    @Test
    public void testTimestampWithStringLiteral() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE hired = '2024-01-01 00:00:00.1'");
        // TODO Check expected distance
    }

    @Test
    public void testTimeWithTimeKeyword() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE TIME(hired) = TIME '00:00:01'");
        // TODO Check expected distance
    }

    @Test
    public void testTimeWithTFormat() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE TIME(hired) = {t '00:00:01'}");
        // TODO Check expected distance
    }

    @Test
    public void testTimeWithStringLiteral() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE TIME(hired) = '00:00:01'");
        // TODO Check expected distance
    }

    @Test
    public void testUnion() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        final double sqlDistance = computeSqlDistance("(SELECT * FROM customers WHERE age > 24) UNION ALL (SELECT * FROM customers WHERE age < 24)");
        // TODO Check expected distance
    }

    @Test
    public void testFromSubqueryWithoutWhereClauseWithResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 23)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'paul', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM (SELECT * FROM customers) c WHERE c.age = 25");
        // TODO Check expected distance
    }

    @Test
    public void testFromSubqueryWithWhereClauseWithResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 23)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'paul', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM (SELECT * FROM customers WHERE name = 'john') c WHERE c.age = 25");
        // TODO Check expected distance
    }

    @Test
    public void testFromSubqueryWithWhereClauseWithoutResults() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 23)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM (SELECT * FROM customers WHERE name = 'john') c WHERE c.age = 25");
        // TODO Check expected distance
    }

    @Test
    public void testFullJoin() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'paul', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");
        final double sqlDistance = computeSqlDistance("SELECT * FROM orders FULL JOIN customers WHERE customers.id = 3");
        // TODO Check expected distance
    }

    @Test
    public void testLeftJoin() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'paul', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM customers LEFT JOIN orders ON orders.customer_id = customers.id WHERE customers.id = 3");
        // TODO Check expected distance
    }


    @Test
    public void testRightJoin() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'paul', 24)");
        final double sqlDistance = computeSqlDistance("SELECT * FROM orders RIGHT JOIN customers ON orders.customer_id = customers.id WHERE customers.id = 3");
        // TODO Check expected distance
    }

    @Test
    public void testCrossJoin() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (2, 'paul', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");

        final double sqlDistance = computeSqlDistance("SELECT * FROM orders CROSS JOIN customers WHERE customers.id = 3");
        // TODO Check expected distance
    }

    @Test
    public void testInnerJoinWithoutWhereClause() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");

        final double sqlDistance = computeSqlDistance("SELECT * FROM orders INNER JOIN customers ON customers.id = 2");
        // TODO Check expected distance
    }

    @Test
    public void testInnerJoinWithWhereClause() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");

        final double sqlDistance = computeSqlDistance("SELECT * FROM orders INNER JOIN customers ON orders.id = customers.id WHERE customers.id = 2");
        // TODO Check expected distance
    }

    @Test
    public void testNullColumn() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, null)");

        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married");
        // TODO Check expected distance
    }

    @Test
    public void testNullColumnComparisonToSingleNull() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, null)");

        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married = false OR married = true");
        // TODO Check expected distance
    }

    @Test
    public void testNullColumnComparisonToNull() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, null)");

        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married=null");
        // TODO Check expected distance
    }

    @Test
    public void testNullColumnIsTrue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO employees VALUES (1, 'john', null, null)");

        final double sqlDistance = computeSqlDistance("SELECT * FROM employees WHERE married IS FALSE OR married IS TRUE");
        // TODO Check expected distance
    }

    @Test
    public void testDelete() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");

        final double sqlDistance = computeSqlDistance("DELETE FROM customers JOIN orders WHERE age = 25");
        // TODO Check expected distance
    }

    @Test
    public void testUpdate() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");

        final double sqlDistance = computeSqlDistance("UPDATE customers SET age = 26 WHERE age = 25");
        // TODO Check expected distance
    }

    @Test
    public void testCaseInsensitiveColumns() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");

        final double sqlDistance = computeSqlDistance("UPDATE customers SET age = 26 WHERE AGE = 25");
        // TODO Check expected distance
    }

    @Test
    public void testAliasedColumns() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");

        final double sqlDistance = computeSqlDistance("SELECT age_alias FROM (SELECT age AS age_alias FROM customers) t WHERE age_alias = 25");
        // TODO Check expected distance
    }

    @Test
    public void testSelectAsValue() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");

        final double sqlDistance = computeSqlDistance("SELECT * FROM customers WHERE age = (SELECT 25) AND (SELECT null) IS TRUE");
        // TODO Check expected distance
    }

    @Test
    public void testBestEffort() throws SQLException {
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO customers VALUES (1, 'john', 24)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO orders VALUES (1, 1, 'guitar')");

        final double sqlDistance = computeSqlDistance("SELECT * FROM (SELECT product FROM customers JOIN orders) t WHERE " +
                "t.product = 'guita'");
        // TODO Check expected distance
    }


}
