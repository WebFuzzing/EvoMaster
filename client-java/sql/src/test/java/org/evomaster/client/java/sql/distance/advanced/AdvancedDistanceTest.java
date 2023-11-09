package org.evomaster.client.java.sql.distance.advanced;

import org.evomaster.client.java.sql.distance.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.distance.advanced.query_distance.Distance;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.distance.advanced.driver.SqlDriver.createSqlDriver;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.Distance.*;
import static org.junit.Assert.assertEquals;

public class AdvancedDistanceTest {

    private static final int DB_PORT = 3306;

    private static SqlDriver sqlDriver;

    @ClassRule
    public static DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yml"))
            .withExposedService("db", DB_PORT);

    @BeforeClass
    public static void beforeClass() {
        sqlDriver = createSqlDriver(
            format("jdbc:mysql://localhost:%s/my_db", DB_PORT), "user", "pass");
    }

    @Before
    public void before() {
        sqlDriver.execute("DELETE FROM payments");
        sqlDriver.execute("DELETE FROM orders");
        sqlDriver.execute("DELETE FROM customers");
    }

    @Test
    public void testQueryWithResults() { //Query returns results
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE age > 20");
        assertEquals(ZERO_DISTANCE, distance);
    }

    @Test
    public void testQueryWithoutResultsWithoutWhere() { //Query doesn't return results and has not where clause
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers");
        assertEquals(INF_DISTANCE, distance);
    }

    @Test
    public void testQueryWithoutResultsWithWhere() { //Query doesn't return results, but due to where clause
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO customers VALUES (2, 'paul', 48)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE age = 25");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testNumber() { //Long
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE age = 25");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testNumber2() { //Double
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE age = 25.0");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testNumber3() { //Signed expressions
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE " +
            "age = +25 AND age=-(-25)");
        assertEquals(createDistance(2D), distance);
    }

    @Test
    public void testAndOperator() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE " +
            "id = 2 AND age = 25");
        assertEquals(createDistance(2D), distance);
    }

    @Test
    public void testOrOperator() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE " +
            "age = 25 OR age = 48");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testArithmeticOperationsAndParenthesis() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE " +
            "age = ((23 - 1 + 1) * 1) / 1");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testBoolean() { //AND and OR operators with boolean literals or boolean columns
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        sqlDriver.execute("INSERT INTO payments VALUES (1, 1, 250, '1968-01-01 00:00:00', true)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM payments WHERE " +
            "amount = 251 AND (true OR FALSE) AND (1 OR 0) AND approved");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testBoolean2() { //Number and boolean comparison
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 24)");
        sqlDriver.execute("INSERT INTO orders VALUES (1, 1, 'guitar')");
        sqlDriver.execute("INSERT INTO payments VALUES (1, 1, 250, '1968-01-01 00:00:00', true)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM payments WHERE " +
            "amount = 251 AND approved = true AND approved = 1");
        assertEquals(createDistance(1D), distance);
    }

    @Test
    public void testComparisonOperators() {
        sqlDriver.execute("INSERT INTO customers VALUES (1, 'john', 19)");
        AdvancedDistance queryDistanceCalculator = new AdvancedDistance(sqlDriver);
        Distance distance = queryDistanceCalculator.calculate("SELECT * FROM customers WHERE " +
            "age = 20 AND age != 19 AND age >= 20 AND age > 19 AND age < 19 AND age <= 18");
        assertEquals(createDistance(6D), distance);
    }
}
