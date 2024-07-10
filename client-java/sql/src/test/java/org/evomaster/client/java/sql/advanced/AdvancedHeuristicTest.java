package org.evomaster.client.java.sql.advanced;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.driver.Schema;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.driver.cache.ConcurrentCache;
import org.evomaster.client.java.sql.advanced.driver.cache.NoCache;
import org.junit.*;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.String.format;
import static java.sql.DriverManager.getConnection;
import static org.evomaster.client.java.distance.heuristics.Truthness.*;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.advanced.AdvancedHeuristic.createAdvancedHeuristic;
import static org.evomaster.client.java.sql.advanced.driver.SqlDriver.createSqlDriver;
import static org.evomaster.client.java.sql.advanced.helpers.dump.JsonFileHelper.loadObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AdvancedHeuristicTest {

    private static final int DB_PORT = 3306;

    private static SqlDriver sqlDriver;

    @ClassRule
    public static DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yml"))
            .withExposedService("db", DB_PORT);

    @BeforeClass
    public static void beforeClass() throws SQLException {
        Connection connection = getConnection(
            format("jdbc:mysql://localhost:%s/my_db", DB_PORT), "user", "pass");
        sqlDriver = createSqlDriver(connection, new NoCache());
    }

    @Before
    public void before() {
        sqlDriver.execute("DELETE FROM orders");
        sqlDriver.execute("DELETE FROM customers");
        sqlDriver.execute("DELETE FROM employees");
    }

    @Test
    public void testQueryWithResults() { //Query returns results
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 50)");
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age > 25");
        assertTrue(truthness.isTrue());
    }

    @Test
    public void testQueryWithoutResultsWithoutWhere() { //Query doesn't return results and has not where clause
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers");
        assertEquals(rowSet(0), truthness);
    }

    @Test
    public void testQueryWithoutResultsWithWhere() { //Query doesn't return results, but due to where clause
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age = 24");
        assertEquals(oneRowWith(singleCondition(eq(25, 24))), truthness);
    }

    @Test
    public void testNumber() { //Long
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age = 24");
        assertEquals(oneRowWith(singleCondition(eq(25, 24))), truthness);
    }

    @Test
    public void testNumber2() { //Double
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age = 24.0");
        assertEquals(oneRowWith(singleCondition(eq(25, 24))), truthness);
    }

    @Test
    public void testNumber3() { //Signed expressions
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age = +24 AND age=-(-24)");
        assertEquals(oneRowWith(andCondition(eq(25, 24), eq(25, 24))), truthness);
    }

    @Test
    public void testString() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE name = 'joh'");
        assertEquals(oneRowWith(singleCondition(eq("john", "joh"))), truthness);
    }

    @Test
    public void testAndOperator() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age = 25");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testOrOperator() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' OR age = 24");
        assertEquals(oneRowWith(orCondition(eq("john", "joh"), eq(100, 99))), truthness);
    }

    @Test
    public void testArithmeticOperationsAndParenthesis() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age = ((24 - 1 + 1) * 1) / 1");
        assertEquals(oneRowWith(singleCondition(eq(25, 24))), truthness);
    }

    @Test
    public void testBoolean() { //Boolean operation with boolean literals
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "true AND FALSE");
        assertEquals(oneRowWith(andCondition(TRUE, FALSE)), truthness);
    }

    @Test
    public void testBoolean2() { //Boolean operation with numbers
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "1 AND 0");
        assertEquals(oneRowWith(andCondition(TRUE, FALSE)), truthness);
    }

    @Test
    public void testBoolean3() { //Boolean column with false
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testBoolean4() { //Boolean column with true
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "name = 'joh' AND married");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testBoolean5() { //Boolean comparison with boolean literal
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married = false");
        assertEquals(oneRowWith(singleCondition(eq(1, 0))), truthness);
    }

    @Test
    public void testBoolean6() { //Boolean comparison with number
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married = 0");
        assertEquals(oneRowWith(singleCondition(eq(1, 0))), truthness);
    }

    @Test
    public void testIsTrue() { //IS TRUE operator with false
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married IS TRUE");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testIsTrue2() { //IS TRUE operator with true
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "name = 'joh' AND married IS TRUE");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testIsNotTrue() { //IS NOT TRUE operator with true
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married IS NOT TRUE");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testIsNotTrue2() { //IS NOT TRUE operator with false
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "name = 'joh' AND married IS NOT TRUE");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testIsFalse() { //IS FALSE operator with true
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married IS FALSE");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testIsFalse2() { //IS FALSE operator with false
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "name = 'joh' AND married IS FALSE");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testIsNotFalse() { //IS NOT FALSE operator with false
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', false, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married IS NOT FALSE");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testIsNotFalse2() { //IS NOT FALSE operator with true
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', true, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "name = 'joh' AND married IS NOT FALSE");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testNot() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 50)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "NOT age > 25");
        assertEquals(oneRowWith(singleCondition(invert(gt(50, 25)))), truthness);
    }

    @Test
    public void testExists() { //Subquery with results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "EXISTS (SELECT client_id FROM orders WHERE product = 'guita')");
        assertEquals(oneRowWith(
            singleCondition(oneRowWith(singleCondition(eq("guitar", "guita"))))), truthness);
    }

    @Test
    public void testExists2() { //Subquery without results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "EXISTS (SELECT client_id FROM orders WHERE client_id = customers.id)");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testNotExists() { //Not exists
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "NOT EXISTS (SELECT client_id FROM orders WHERE product = 'guita')");
        assertEquals(oneRowWith(
            singleCondition(invert(oneRowWith(singleCondition(eq("guitar", "guita")))))), truthness);
    }

    @Test
    public void testIn() { //List of values
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age IN (25, 1)");
        assertEquals(oneRowWith(anyCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testIn2() { //Subquery with results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age IN (SELECT 25 UNION SELECT 1)");
        assertEquals(oneRowWith(anyCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testIn3() { //Subquery without results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "id IN (SELECT client_id FROM orders)");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testNotIn() { //List of values
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age NOT IN (24, 100)");
        assertEquals(oneRowWith(allCondition(notEq(24, 24))), truthness);
    }

    @Test
    public void testNotIn2() { //Subquery with results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age NOT IN (SELECT 24 UNION SELECT 1)");
        assertEquals(oneRowWith(allCondition(notEq(24, 24))), truthness);
    }

    @Test
    public void testNotIn3() { //Subquery without results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "id NOT IN (SELECT client_id FROM orders)");
        assertEquals(oneRowWith(singleCondition(TRUE)), truthness);
    }

    @Test
    public void testAny() { //Subquery with results, satisfied
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age = ANY(SELECT 24)");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testAny2() { //Subquery with results, unsatisfied
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age = ANY(SELECT 25 UNION SELECT 1)");
        assertEquals(oneRowWith(anyCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testAny3() { //Subquery without results (and using 'SOME' keyword)
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "id = ANY(SELECT client_id FROM orders)");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testAll() { //Subquery with results, satisfied
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age = ALL(SELECT 24)");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testAll2() { //Subquery with results, unsatisfied
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age = ALL(SELECT 25 UNION SELECT 1)");
        assertEquals(oneRowWith(allCondition(eq(24, 1))), truthness);
    }

    @Test
    public void testAll3() { //Subquery without results (and using 'SOME' keyword)
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "id = ALL(SELECT client_id FROM orders)");
        assertEquals(oneRowWith(singleCondition(TRUE)), truthness);
    }

    @Test
    public void testIsNull() { //Value is null
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age IS NULL");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testIsNull2() { //Value is not null
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age IS NULL");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testIsNotNull() { //Value is null
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 25)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age IS NOT NULL");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testIsNotNull2() { //Value is not null
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age IS NOT NULL");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testBetween() { //In range
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age BETWEEN 22 AND 25");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testBetween2() { //Not in range
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age BETWEEN 26 AND 30");
        assertEquals(oneRowWith(andCondition(gte(24, 26), lte(24, 30))), truthness);
    }

    @Test
    public void testNotBetween() { //Not in range
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "name = 'joh' AND age NOT BETWEEN 26 AND 30");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testNotBetween2() { //In range
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "age NOT BETWEEN 22 AND 25");
        assertEquals(oneRowWith(orCondition(lt(23, 22), gt(23, 25))), truthness);
    }

    @Test
    public void testComparisonOperators() { //Equals
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age = 20");
        assertEquals(oneRowWith(singleCondition(eq(19, 20))), truthness);
    }

    @Test
    public void testComparisonOperators2() { //Not equals
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age != 19");
        assertEquals(oneRowWith(singleCondition(notEq(19, 19))), truthness);
    }

    @Test
    public void testComparisonOperators3() { //Greater than or equals
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age >= 20");
        assertEquals(oneRowWith(singleCondition(gte(19, 20))), truthness);
    }

    @Test
    public void testComparisonOperators4() { //Greater than
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age > 19");
        assertEquals(oneRowWith(singleCondition(gt(19, 19))), truthness);
    }

    @Test
    public void testComparisonOperators5() { //Less than or equals
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age <= 18");
        assertEquals(oneRowWith(singleCondition(lte(19, 18))), truthness);
    }

    @Test
    public void testComparisonOperators6() { //Less than
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE age < 19");
        assertEquals(oneRowWith(singleCondition(lt(19, 19))), truthness);
    }

    @Test
    public void testAlias() { //Not using 'AS' keyword
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers c WHERE " +
            "name = 'joh' AND EXISTS (SELECT client_id FROM orders WHERE client_id = c.id)");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testAlias2() { //Using 'AS' keyword
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers AS c " +
            "WHERE name = 'joh' AND EXISTS (SELECT client_id FROM orders WHERE client_id = c.id)");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testAlias3() { //Using table name instead of alias
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers " +
            "WHERE name = 'joh' AND EXISTS (SELECT client_id FROM orders WHERE client_id = customers.id)");
        assertEquals(oneRowWith(andCondition(eq("john", "joh"), TRUE)), truthness);
    }

    @Test
    public void testFunction() { //Simplest case
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "UPPER(name) = 'JOH'");
        assertEquals(oneRowWith(singleCondition(eq("john", "joh"))), truthness);
    }

    @Test
    public void testFunction2() { //Function in sub query using top level query column as parameter
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john lennon', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'george harrison', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 2, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM customers WHERE " +
            "EXISTS (SELECT client_id FROM orders WHERE UPPER(name) = 'GEORGE HARRISO')");
        assertEquals(
            andCondition(rowSet(2),
                oneRowWith(singleCondition(eq("GEORGE HARRISON", "GEORGE HARRISO")))), truthness);
    }

    @Test
    public void testGroupBy() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT COUNT(*) FROM customers WHERE " +
            "name = 'joh' GROUP BY age");
        assertEquals(oneRowWith(singleCondition(eq("john", "joh"))), truthness);
    }

    @Test
    public void testDate() { //Date with DATE keyword
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:01')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "hired = DATE '2024-01-01'");
        assertEquals(oneRowWith(singleCondition(eq(1000,0))), truthness);
    }

    @Test
    public void testDate2() { //Date with d format
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:01')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "hired = {d '2024-01-01'}");
        assertEquals(oneRowWith(singleCondition(eq(1000,0))), truthness);
    }

    @Test
    public void testDate3() { //Date with string literal
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:01')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "hired = '2024-01-01 00:00:00'");
        assertEquals(oneRowWith(singleCondition(eq(1000,0))), truthness);
    }

    @Test
    public void testTimestamp() { //Timestamp with TIMESTAMP keyword
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "hired = TIMESTAMP '2024-01-01 00:00:00.1'");
        assertEquals(oneRowWith(singleCondition(eq(100,0))), truthness);
    }

    @Test
    public void testTimestamp2() { //Timestamp with ts format
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "hired = {ts '2024-01-01 00:00:00.1'}");
        assertEquals(oneRowWith(singleCondition(eq(100,0))), truthness);
    }

    @Test
    public void testTimestamp3() { //Timestamp with string literal
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "hired = '2024-01-01 00:00:00.1'");
        assertEquals(oneRowWith(singleCondition(eq(100,0))), truthness);
    }

    @Test
    public void testTime() { //Time with TIME keyword
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "TIME(hired) = TIME '00:00:01'");
        assertEquals(oneRowWith(singleCondition(eq(1000,0))), truthness);
    }

    @Test
    public void testTime2() { //Time with t format
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "TIME(hired) = {t '00:00:01'}");
        assertEquals(oneRowWith(singleCondition(eq(1000,0))), truthness);
    }

    @Test
    public void testTime3() { //Time with string literal
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, '2024-01-01 00:00:00')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "TIME(hired) = '00:00:01'");
        assertEquals(oneRowWith(singleCondition(eq(1000,0))), truthness);
    }

    @Test
    public void testUnion() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate(
    "(SELECT * FROM customers WHERE age = 24) UNION ALL (SELECT * FROM customers WHERE age > 24)");
        assertEquals(orAggregation(
            oneRowWith(singleCondition(eq(24,24))),
            oneRowWith(singleCondition(gt(24,24)))), truthness);
    }

    @Test
    public void testFromSubquery() { //Subquery with results (subquery without WHERE clause)
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 23)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM " +
            "(SELECT * FROM customers) c WHERE c.age = 25");
        assertEquals(andAggregation(rowSet(2), singleCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testFromSubquery2() { //Subquery with results (subquery with WHERE clause)
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 23)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM " +
            "(SELECT * FROM customers WHERE name = 'john') c WHERE c.age = 25");
        assertEquals(andAggregation(andAggregation(rowSet(2), singleCondition(TRUE)), singleCondition(eq(23, 25))), truthness);
    }

    @Test
    public void testFromSubquery3() { //Subquery without results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 23)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM " +
            "(SELECT * FROM customers WHERE name = 'joh') c WHERE c.age = 25");
        assertEquals(andAggregation(oneRowWith(singleCondition(eq("john", "joh"))), FALSE), truthness);
    }

    @Test
    public void testJoin() { //Full join
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM orders FULL JOIN customers");
        assertEquals(orAggregation(rowSet(1), rowSet(2)), truthness);
    }

    @Test
    public void testJoin2() { //Left join
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM orders LEFT JOIN customers");
        assertEquals(rowSet(1), truthness);
    }

    @Test
    public void testJoin3() { //Right join
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM orders RIGHT JOIN customers");
        assertEquals(rowSet(2), truthness);
    }

    @Test
    public void testJoin4() { //Cross join
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM orders CROSS JOIN customers");
        assertEquals(andAggregation(rowSet(1), rowSet(2)), truthness);
    }

    @Test
    public void testJoin5() { //Inner join without WHERE clause
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness queryTruthness = advancedHeuristic.calculate("SELECT * FROM orders INNER JOIN customers " +
            "ON orders.id = customers.id");
        Truthness crossJoinTruthness = andAggregation(andAggregation(rowSet(1), rowSet(2)), TRUE);
        assertEquals(andAggregation(rowSet(1), rowSet(2), crossJoinTruthness), queryTruthness);
    }

    @Test
    public void testJoin6() { //Inner join with WHERE clause
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness queryTruthness = advancedHeuristic.calculate("SELECT * FROM orders INNER JOIN customers " +
            "ON orders.id = customers.id WHERE name = 'joh'");
        Truthness crossJoinTruthness = andAggregation(andAggregation(rowSet(1), rowSet(2)), TRUE);
        Truthness joinTruthness = andAggregation(rowSet(1), rowSet(2), crossJoinTruthness);
        Truthness conditionTruthness = singleCondition(eq("john", "joh"));
        assertEquals(andAggregation(joinTruthness, conditionTruthness), queryTruthness);
    }

    @Test
    public void testNull() { //Null column
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE married");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testNull2() { //Comparison with single null
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
        "married = false OR married = true");
        assertEquals(oneRowWith(orCondition(FALSE_BETTER, FALSE_BETTER)), truthness);
    }

    @Test
    public void testNull3() { //Comparison with two nulls
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married=null");
        assertEquals(oneRowWith(singleCondition(FALSE)), truthness);
    }

    @Test
    public void testNull4() { //IS TRUE/FALSE operators with null
        sqlDriver.execute("INSERT INTO employees VALUES (1, 'john', null, null)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM employees WHERE " +
            "married IS FALSE OR married IS TRUE");
        assertEquals(oneRowWith(orCondition(FALSE, FALSE)), truthness);
    }

    @Test
    public void testDelete() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("DELETE FROM customers JOIN orders WHERE " +
            "age = 25");
        assertEquals(oneRowWith(singleCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testUpdate() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("UPDATE customers SET age = 26 WHERE " +
            "age = 25");
        assertEquals(oneRowWith(singleCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testCaseInsensitiveColumns() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("UPDATE customers SET age = 26 WHERE " +
            "AGE = 25");
        assertEquals(oneRowWith(singleCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testAliasedColumns() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT age_alias FROM (SELECT age AS age_alias FROM customers) t WHERE " +
            "age_alias = 25");
        assertEquals(oneRowWith(singleCondition(eq(24, 25))), truthness);
    }

    @Test
    public void testBestEffort() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        Truthness truthness = advancedHeuristic.calculate("SELECT * FROM (SELECT product FROM customers JOIN orders) t WHERE " +
            "t.product = 'guita'");
        assertEquals(oneRowWith(singleCondition(eq("guitar", "guita"))), truthness);
    }

    @Ignore("Test template for issues")
    @Test
    public void testIssue() {
        restoreState("cache_file.txt", "schema_file.txt", sqlDriver);
        AdvancedHeuristic advancedHeuristic = createAdvancedHeuristic(sqlDriver);
        advancedHeuristic.calculate("query");
    }

    private static void restoreState(String cacheFile, String schemaFile, SqlDriver sqlDriver){
        sqlDriver.setCache(loadObject(cacheFile, ConcurrentCache.class));
        sqlDriver.setSchema(loadObject(schemaFile, Schema.class));
    }

    private static Truthness andCondition(Truthness truthness1, Truthness truthness2){
        return trueOrScaleTrue(andAggregation(truthness1, truthness2).getOfTrue());
    }

    private static Truthness orCondition(Truthness truthness1, Truthness truthness2){
        return trueOrScaleTrue(orAggregation(truthness1, truthness2).getOfTrue());
    }

    private static Truthness singleCondition(Truthness truthness){
        return trueOrScaleTrue(truthness.getOfTrue());
    }

    private static Truthness anyCondition(Truthness max){
        return singleCondition(trueOrScaleTrue(max.getOfTrue()));
    }

    private static Truthness allCondition(Truthness min){
        return singleCondition(trueOrScaleTrue(min.getOfTrue()));
    }

    private static Truthness rowSet(Integer size){
        return getTruthnessToEmpty(size).invert();
    }

    private static Truthness oneRowWith(Truthness condition){
        return andAggregation(rowSet(1), condition);
    }

    public static Truthness eq(Integer left, Integer right) {
        return trueOrScaleTrue(getEqualityTruthness(left, right).getOfTrue());
    }

    public static Truthness notEq(Integer left, Integer right) {
        return trueOrScaleTrue(getEqualityTruthness(left, right).invert().getOfTrue());
    }

    public static Truthness gt(Integer left, Integer right) {
        return trueOrScaleTrue(getLessThanTruthness(right, left).getOfTrue());
    }

    public static Truthness gte(Integer left, Integer right) {
        return trueOrScaleTrue(getLessThanTruthness(left, right).invert().getOfTrue());
    }

    public static Truthness lt(Integer left, Integer right) {
        return trueOrScaleTrue(getLessThanTruthness(left, right).getOfTrue());
    }

    public static Truthness lte(Integer left, Integer right) {
        return trueOrScaleTrue(getLessThanTruthness(right, left).invert().getOfTrue());
    }

    public static Truthness eq(String left, String right) {
        return trueOrScaleTrue(getStringEqualityTruthness(left, right).getOfTrue());
    }

    public static Truthness invert(Truthness truthness) {
        return truthness.invert();
    }
}
