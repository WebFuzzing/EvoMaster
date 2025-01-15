package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.*;
import java.sql.Timestamp;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlHeuristicsCalculatorTest {

    @Test
    public void testNoWhereNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Arrays.asList("name"), "Person");
        queryResult.addRow(new DataRow("name","John", "Person"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testWhereNoFromClause() {
        String sqlCommand = "SELECT 1 AS example_column WHERE 1 = 0";
        QueryResult virtualTableContents = new QueryResult(Arrays.asList("example_column"), null);
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, virtualTableContents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testNoWhereNoFromClause() {
        String sqlCommand = "SELECT 1 AS example_column";
        QueryResult virtualTableContents = new QueryResult(Arrays.asList("example_column"), null);
        virtualTableContents.addRow(new DataRow("example_column",1, null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, virtualTableContents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testNoWhereNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Arrays.asList("name"), "Person");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightOuterJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightOuterJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectEqualsTo() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age=18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectMinorThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness(33.0d, 18.0d).getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectMinorThanEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<=18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness(18.0d, 33.0d).invert().getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectGreaterThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>age";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness( 33,18.0d).getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectGreaterThanEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>=age";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness( 18,33.0d).invert().getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectNotEqualsTo() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age!=18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = TruthnessUtils.getEqualityTruthness( 18d,18d).invert().getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForSelectNotEqualsToCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age!=18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForSelectGreaterThanEqualsCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>=age";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForSelectGreaterThanCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>age";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForSelectMinorThanEqualsCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<=18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 15));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForSelectMinorThanCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForStringMatching() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name='John'";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnNonZeroDistanceForStringMatching() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name='John'";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("Jack", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = SqlExpressionEvaluator.getEqualityTruthness( "John","Jack").getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForStringsNotMatching() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name!='John'";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("Jack", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }



    @Test
    public void testShouldReturnNoneroDistanceForStringsNotMatching() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name!='John'";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = SqlExpressionEvaluator.getEqualityTruthness( "John","John").invert().getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForBooleanMatching() {
        String sqlCommand = "SELECT name, age, is_member FROM Persons WHERE is_member=true";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age","is_member"), "Persons");
        queryResult.addRow(Arrays.asList("name","age", "is_member"),"Persons",Arrays.asList("John", 18, true));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForIsNull() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList(null, 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForIsNotNull() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name IS NOT NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnNonZeroDistanceForIsNull() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double equalityTruthness = SqlExpressionEvaluator.getTruthnessToIsNull( "John").getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, equalityTruthness).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForDoubleComparison() {
        String sqlCommand = "SELECT price FROM Products WHERE price<=19.99";
        QueryResult queryResult = new QueryResult(Arrays.asList("price"), "Products");
        queryResult.addRow(Arrays.asList("price"),"Products",Arrays.asList(9.99));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForAddition() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary+bonus > 50000";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(40000,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForDivision() {
        String sqlCommand = "SELECT name, salary " +
                "    FROM Employees " +
                "    WHERE salary / 12 > 3000";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","salary"), "Employees");
        queryResult.addRow(Arrays.asList("name","salary"),"Employees",Arrays.asList("John",48000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForMultiplication() {
        String sqlCommand = "SELECT product_name, price, quantity " +
                "    FROM Products " +
                "    WHERE price * quantity > 100";
        QueryResult queryResult = new QueryResult(Arrays.asList("product_name","price","quantity"), "Products");
        queryResult.addRow(Arrays.asList("product_name","price","quantity"),"Products",Arrays.asList("Laptop",120,2));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForSubstraction() {
        String sqlCommand = "SELECT product_name, original_price, discount_price " +
                "    FROM Products " +
                "    WHERE original_price - discount_price > 20";
        QueryResult queryResult = new QueryResult(Arrays.asList("product_name","original_price","discount_price"), "Products");
        queryResult.addRow(Arrays.asList("product_name","original_price","discount_price"),"Products",Arrays.asList("Laptop",300,200));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForIntegerDivision() {
        String sqlCommand = "SELECT name, salary " +
                "    FROM Employees " +
                "    WHERE salary DIV 12 > 3000";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","salary"), "Employees");
        queryResult.addRow(Arrays.asList("name","salary"),"Employees",Arrays.asList("John",48000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForPlusSignedExpression() {
        String sqlCommand = "SELECT price " +
                "    FROM Products " +
                "    WHERE +price > 100";
        QueryResult queryResult = new QueryResult(Arrays.asList("price"), "Products");
        queryResult.addRow(Arrays.asList("price"),"Products",Arrays.asList(200));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForMinusSignedExpression() {
        String sqlCommand = "SELECT price " +
                "    FROM Products " +
                "    WHERE -price > 100";
        QueryResult queryResult = new QueryResult(Arrays.asList("price"), "Products");
        queryResult.addRow(Arrays.asList("price"),"Products",Arrays.asList(-200));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForBitwiseNotSignedExpression() {
        assertEquals(0,~(-1));
        String sqlCommand = "SELECT permissions " +
                "    FROM Users " +
                "    WHERE ~permissions = 0 ";
        QueryResult queryResult = new QueryResult(Arrays.asList("permissions"), "Users");

        queryResult.addRow(Arrays.asList("permissions"),"Users",Arrays.asList(-1));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForTimestamp() {
        String sqlCommand = "SELECT order_id, customer_id, order_timestamp " +
                "    FROM Orders " +
                "    WHERE order_timestamp = '2025-01-14 12:30:45' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id","customer_id","order_timestamp"), "Orders");

        String timestampString = "2025-01-14 12:30:45";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Parse the string into a LocalDateTime object
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);

        // Convert the LocalDateTime to Timestamp
        Timestamp timestamp = Timestamp.valueOf(localDateTime);

        queryResult.addRow(Arrays.asList("order_id","customer_id","order_timestamp"),"Orders",Arrays.asList(1,1,timestamp));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForDate() {
        String sqlCommand = "SELECT event_id, event_date " +
                " FROM Events " +
                " WHERE event_date = '2025-01-14' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("event_id","event_date"), "Events");

        LocalDate date = LocalDate.of(2025, 1, 14);
        queryResult.addRow(Arrays.asList("event_id","event_date"),"Events",Arrays.asList(1,date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForTime() {
        String sqlCommand = "SELECT schedule_id, start_time " +
                " FROM Schedules " +
                " WHERE start_time = '12:30:45' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("schedule_id","start_time"), "Schedules");
        LocalTime time = LocalTime.of(12, 30, 45);
        queryResult.addRow(Arrays.asList("schedule_id","start_time"),"Schedules",Arrays.asList(1,time));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForDateTime() {
        String sqlCommand = "SELECT appointment_id, appointment_datetime " +
                " FROM Appointments " +
                " WHERE appointment_datetime = '2025-01-14 12:30:45' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("appointment_id","appointment_datetime"), "Appointments");
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 14, 12, 30, 45);
        queryResult.addRow(Arrays.asList("appointment_id", "appointment_datetime"),"Appointments",Arrays.asList(1,dateTime));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForYear() {
        String sqlCommand = " SELECT person_id, birth_year " +
                " FROM Birthdays " +
                " WHERE birth_year = 2025 ";
        QueryResult queryResult = new QueryResult(Arrays.asList("person_id", "birth_year"), "Birthdays");
        Year year = Year.of(2025);
        queryResult.addRow(Arrays.asList("person_id", "birth_year"),"Birthdays",Arrays.asList(1,year));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForTimeWithTimeZone() {
        String sqlCommand = "SELECT event_id, event_time " +
                " FROM Events " +
                " WHERE event_time = '12:30:45+02:00' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("event_id", "event_time"), "Events");

        OffsetTime offsetTime = OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2));
        queryResult.addRow(Arrays.asList("event_id", "event_time"),"Events",Arrays.asList(1,offsetTime));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForTimeStampWithTimeZone() {
        String sqlCommand = " SELECT event_id, event_time " +
                " FROM Events " +
                " WHERE event_time = '12:30:45+02:00' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("event_id", "event_time"), "Events");
        OffsetTime offsetTime = OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2));
        queryResult.addRow(Arrays.asList("event_id", "event_time"),"Events",Arrays.asList(1,offsetTime));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }
}
