package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.sql.Timestamp;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlHeuristicsCalculatorTest {

    @Test
    public void testNoWhereNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Person");
        queryResult.addRow(new DataRow("name","John", "Person"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testWhereNoFromClause() {
        String sqlCommand = "SELECT 1 AS example_column WHERE 1 = 0";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, virtualTableContents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testNoWhereNoFromClause() {
        String sqlCommand = "SELECT 1 AS example_column";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        virtualTableContents.addRow(new DataRow("example_column",1, null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, virtualTableContents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testNoWhereNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Person");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightOuterJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightOuterJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        tableBcontents.addRow(new DataRow("name","John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinNoFromTableNoRows() {
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult tableBcontents = new QueryResult(Collections.singletonList("name"), "TableB");
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
    public void testShouldReturnZeroDistanceForIsTrue() {
        String sqlCommand = "SELECT name, age, is_member FROM Persons WHERE is_member IS TRUE";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age","is_member"), "Persons");
        queryResult.addRow(Arrays.asList("name","age", "is_member"),"Persons",Arrays.asList("John", 18, true));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForIsFalse() {
        String sqlCommand = "SELECT name, age, is_member FROM Persons WHERE is_member IS FALSE";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age","is_member"), "Persons");
        queryResult.addRow(Arrays.asList("name","age", "is_member"),"Persons",Arrays.asList("John", 18, false));
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
        QueryResult queryResult = new QueryResult(Collections.singletonList("price"), "Products");
        queryResult.addRow(Collections.singletonList("price"),"Products",Collections.singletonList(9.99));
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
    public void testShouldReturnZeroDistanceForSubtraction() {
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
        QueryResult queryResult = new QueryResult(Collections.singletonList("price"), "Products");
        queryResult.addRow(Collections.singletonList("price"),"Products",Collections.singletonList(200));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForMinusSignedExpression() {
        String sqlCommand = "SELECT price " +
                "    FROM Products " +
                "    WHERE -price > 100";
        QueryResult queryResult = new QueryResult(Collections.singletonList("price"), "Products");
        queryResult.addRow(Collections.singletonList("price"),"Products",Collections.singletonList(-200));
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
        QueryResult queryResult = new QueryResult(Collections.singletonList("permissions"), "Users");

        queryResult.addRow(Collections.singletonList("permissions"),"Users",Collections.singletonList(-1));
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
    public void testShouldReturnZeroDistanceForDate() throws ParseException {
        String sqlCommand = "SELECT event_id, event_date " +
                " FROM Events " +
                " WHERE event_date = '2025-01-14' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("event_id","event_date"), "Events");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date date = dateFormat.parse("14/01/2025");
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
        Time time = Time.valueOf("12:30:45");
        queryResult.addRow(Arrays.asList("schedule_id","start_time"),"Schedules",Arrays.asList(1,time));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForDateTime() throws ParseException {
        String sqlCommand = "SELECT appointment_id, appointment_datetime " +
                " FROM Appointments " +
                " WHERE appointment_datetime = '2025-01-14 12:30:45' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("appointment_id","appointment_datetime"), "Appointments");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateTime = sdf.parse("2025-01-14 12:30:45");
        queryResult.addRow(Arrays.asList("appointment_id", "appointment_datetime"),"Appointments",Arrays.asList(1,dateTime));

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
                " WHERE event_time = '2025-01-14 12:30:45+02:00' ";
        QueryResult queryResult = new QueryResult(Arrays.asList("event_id", "event_time"), "Events");

        // Define the string with date, time, and timezone offset
        String dateTimeString = "2025-01-14 12:30:45+02:00";

        // Define the DateTimeFormatter to match the input string pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");

        // Parse the string into a ZonedDateTime instance
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString, formatter);


        queryResult.addRow(Arrays.asList("event_id", "event_time"),"Events",Arrays.asList(1,offsetDateTime));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testShouldReturnZeroDistanceForYear() throws ParseException {
        String sqlCommand = "SELECT employee_id, hire_year " +
                " FROM employees " +
                " WHERE hire_year = 2018 ";
        QueryResult queryResult = new QueryResult(Arrays.asList("employee_id", "hire_year"), "employees");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date hire_year = sdf.parse("2018-01-01");

        queryResult.addRow(Arrays.asList("employee_id", "hire_year"),"employees",Arrays.asList(1,hire_year));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testDateTimeLiteralExpression() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = TIMESTAMP '2025-01-22 15:30:45'";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_date"), "orders");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date order_date = sdf.parse("2025-01-22 15:30:45");

        queryResult.addRow(Arrays.asList("order_id", "order_date"), "orders", Arrays.asList(1, order_date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testTimestampValue() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = {ts '2025-01-22 15:30:45'}";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_date"), "orders");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date order_date = sdf.parse("2025-01-22 15:30:45");

        queryResult.addRow(Arrays.asList("order_id", "order_date"), "orders", Arrays.asList(1, order_date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testDateLiteralExpression() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = DATE '2025-01-22'";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_date"), "orders");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date order_date = sdf.parse("2025-01-22");

        queryResult.addRow(Arrays.asList("order_id", "order_date"), "orders", Arrays.asList(1, order_date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testDateValue() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = {d '2025-01-22'}";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_date"), "orders");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date order_date = sdf.parse("2025-01-22");

        queryResult.addRow(Arrays.asList("order_id", "order_date"), "orders", Arrays.asList(1, order_date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testTimeLiteralExpression() {
        String sqlCommand = "SELECT * FROM orders WHERE order_time = TIME '15:30:45'";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_time"), "orders");

        Time order_time = Time.valueOf("15:30:45");

        queryResult.addRow(Arrays.asList("order_id", "order_time"), "orders", Arrays.asList(1, order_time));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testTimeValue() {
        String sqlCommand = "SELECT * FROM orders WHERE order_time = {t '15:30:45'}";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_time"), "orders");

        Time order_time = Time.valueOf("15:30:45");

        queryResult.addRow(Arrays.asList("order_id", "order_time"), "orders", Arrays.asList(1, order_time));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testTimestampWithTimeZoneLiteralExpression() {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = TIMESTAMPTZ '2025-01-22 15:30:45+02:00'";
        QueryResult queryResult = new QueryResult(Arrays.asList("order_id", "order_date"), "orders");

        String timestampString = "2025-01-22 15:30:45+02:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
        OffsetDateTime order_date = OffsetDateTime.parse(timestampString, formatter);

        queryResult.addRow(Arrays.asList("order_id", "order_date"), "orders", Arrays.asList(1, order_date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testParenthesis() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE (salary+bonus) > 50000";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(40000,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testAndCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age>18 AND age<30";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 25));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testOrCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18 OR age>30";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 17));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBetweenNumbers() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age BETWEEN 18 AND 30";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 23));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBetweenStrings() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name BETWEEN 'A' AND 'Z'";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 23));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBetweenDates() {
        String sqlCommand = "SELECT birth_day FROM Persons WHERE birth_day BETWEEN '1990-07-01' AND '1990-07-31'";
        QueryResult queryResult = new QueryResult(Collections.singletonList("birth_day"), "Persons");
        java.sql.Date date = java.sql.Date.valueOf("1990-07-15");
        queryResult.addRow(Collections.singletonList("birth_day"),"Persons",Collections.singletonList(date));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBetweenTimes() {
        String sqlCommand = "SELECT start_time FROM Schedules WHERE start_time BETWEEN '09:00:00' AND '17:00:00'";
        QueryResult queryResult = new QueryResult(Collections.singletonList("start_time"), "Schedules");
        java.sql.Time time = Time.valueOf("12:30:45");
        queryResult.addRow(Collections.singletonList("start_time"),"Schedules",Collections.singletonList(time));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBetweenTimestamps() {
        String sqlCommand = "SELECT event_timestamp FROM Events WHERE event_timestamp BETWEEN '2023-01-01 00:00:00' AND '2023-12-31 23:59:59'";
        QueryResult queryResult = new QueryResult(Collections.singletonList("event_timestamp"), "Events");

        String timestampString = "2023-01-14 12:30:45";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        Timestamp timestamp = Timestamp.valueOf(localDateTime);

        queryResult.addRow(Collections.singletonList("event_timestamp"),"Events",Collections.singletonList(timestamp));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testXOrCondition() {
        String sqlCommand = "SELECT salary, age FROM Employees WHERE (age > 30) XOR (salary > 50000)";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","age"), "Employees");
        queryResult.addRow(Arrays.asList("salary","age"),"Persons",Arrays.asList(40000, 35));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testModulo() {
        String sqlCommand = "SELECT salary FROM Employees WHERE (salary % 2) = 0";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(40000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBitwiseRightShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary >> 1) > 20000";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(40010));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBitwiseLeftShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary << 1) > 20000";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(10005));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullValue() {
        String sqlCommand = "SELECT * FROM Employees WHERE salary = NULL";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);

        Truthness scaledFalseTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, SqlHeuristicsCalculator.FALSE_TRUTHNESS.getOfTrue());
        double expectedDistance = 1 - scaledFalseTruthness.getOfTrue();
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testHexValue() {
        String sqlCommand = "SELECT * FROM Employees WHERE salary= 0x1A";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(26));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testOverlapsTime() {
        String sqlCommand = "SELECT start_time, end_time FROM Events WHERE (start_time, end_time) OVERLAPS (TIME '10:00:00', TIME '12:00:00') ";
        QueryResult queryResult = new QueryResult(Arrays.asList("start_time","end_time"), "Events");
        Time start_time = Time.valueOf("10:30:00");
        Time end_time = Time.valueOf("13:00:00");
        queryResult.addRow(Arrays.asList("start_time","end_time"),"Events",Arrays.asList(start_time,end_time));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testOverlapsDate() {
        String sqlCommand = "SELECT start_date, end_date FROM Events WHERE (start_date, end_date) OVERLAPS (DATE '2023-01-01', DATE '2024-01-01') ";
        QueryResult queryResult = new QueryResult(Arrays.asList("start_date","end_date"), "Events");
        java.sql.Date start_date = java.sql.Date.valueOf("2023-05-01");
        java.sql.Date end_date = java.sql.Date.valueOf("2024-01-10");
        queryResult.addRow(Arrays.asList("start_date","end_date"),"Events",Arrays.asList(start_date,end_date));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testOverlapsTimestamp() {
        String sqlCommand = "SELECT start_timestamp, end_timestamp FROM Events WHERE (start_timestamp, end_timestamp) OVERLAPS (TIMESTAMP '2023-01-01 00:00:00', TIMESTAMP '2024-01-01 23:59:59') ";
        QueryResult queryResult = new QueryResult(Arrays.asList("start_timestamp","end_timestamp"), "Events");
        Timestamp start_timestamp = Timestamp.valueOf("2023-05-01 00:00:00");
        Timestamp end_timestamp = Timestamp.valueOf("2024-01-10 23:59:59");
        queryResult.addRow(Arrays.asList("start_timestamp","end_timestamp"),"Events",Arrays.asList(start_timestamp,end_timestamp));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNotExpression() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE NOT age=18";
        QueryResult queryResult = new QueryResult(Arrays.asList("name","age"), "Persons");
        queryResult.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 23));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testConcat() {
        String sqlCommand = "SELECT * FROM Employees WHERE first_name || ' ' || last_name = 'John Doe'";
        QueryResult queryResult = new QueryResult(Arrays.asList("first_name", "last_name"), "Employees");
        queryResult.addRow(Arrays.asList("first_name", "last_name"), "Employees", Arrays.asList("John", "Doe"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBitwiseAnd() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary & 1) = 1";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"), "Employees", Collections.singletonList(1));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testBitwiseOr() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary | 1) = 3";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"), "Employees", Collections.singletonList(2));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testBitwiseXor() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary ^ 2) = 3";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"), "Employees", Collections.singletonList(1));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRegExpMatchOperator() {
        String sqlCommand = "SELECT * FROM Employees WHERE name ~ 'John.*'";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Employees");
        queryResult.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John Doe"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSimilarToExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE name SIMILAR TO 'John%'";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Employees");
        queryResult.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John Doe"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testArrayConstructor() {
        String sqlCommand = "SELECT * FROM Employees WHERE ARRAY[salary, bonus] = ARRAY[50000, 5000]";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary", "bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary", "bonus"), "Employees", Arrays.asList(50000, 5000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testArrayExpressionWithIndex() {
        String sqlCommand = "SELECT * FROM Employees WHERE ARRAY[salary, bonus][1] = 50000";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary", "bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary", "bonus"), "Employees", Arrays.asList(50000, 5000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testArrayExpressionUsingStartEndIndexes() {
        String sqlCommand = "SELECT * FROM Employees WHERE ARRAY[salary, bonus][2:2] = ARRAY[5000]";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary", "bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary", "bonus"), "Employees", Arrays.asList(50000, 5000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testArrayExpressionUsingStartEndIndexesAndIndex() {
        String sqlCommand = "SELECT * FROM Employees WHERE (ARRAY[salary, bonus][2:2])[1] = 5000";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary", "bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary", "bonus"), "Employees", Arrays.asList(50000, 5000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullAddition() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary+bonus IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(null,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullSubtracion() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary-bonus IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(null,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullBitwiseOr() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary | bonus IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(null,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullBitwiseAnd() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary & bonus IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(null,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullBitwiseXor() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary ^ bonus IS NULL";
        QueryResult queryResult = new QueryResult(Arrays.asList("salary","bonus"), "Employees");
        queryResult.addRow(Arrays.asList("salary","bonus"),"Employees",Arrays.asList(null,20000));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullModulo() {
        String sqlCommand = "SELECT salary FROM Employees WHERE (salary % 2) IS NULL";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullBitwiseRightShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary >> 1) IS NULL";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullBitwiseLeftShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary << 1) IS NULL";
        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(Collections.singletonList("salary"),"Employees",Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullSignedExpression() {
        String sqlCommand = "SELECT price " +
                "    FROM Products " +
                "    WHERE +price IS NULL";
        QueryResult queryResult = new QueryResult(Collections.singletonList("price"), "Products");
        queryResult.addRow(Collections.singletonList("price"),"Products",Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullBetweenTimestamps() {
        String sqlCommand = "SELECT event_timestamp FROM Events WHERE NOT (event_timestamp BETWEEN NULL AND '2023-12-31 23:59:59')";
        QueryResult queryResult = new QueryResult(Collections.singletonList("event_timestamp"), "Events");

        String timestampString = "2023-01-14 12:30:45";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        Timestamp timestamp = Timestamp.valueOf(localDateTime);

        queryResult.addRow(Collections.singletonList("event_timestamp"),"Events",Collections.singletonList(timestamp));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullOverlapsTime() {
        String sqlCommand = "SELECT start_time, end_time FROM Events WHERE NOT ((start_time, end_time) OVERLAPS (NULL, TIME '12:00:00')) ";
        QueryResult queryResult = new QueryResult(Arrays.asList("start_time","end_time"), "Events");
        Time start_time = Time.valueOf("10:30:00");
        Time end_time = Time.valueOf("13:00:00");
        queryResult.addRow(Arrays.asList("start_time","end_time"),"Events",Arrays.asList(start_time,end_time));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 0;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullConcat() {
        String sqlCommand = "SELECT * FROM Employees WHERE first_name || ' ' || last_name IS NULL ";
        QueryResult queryResult = new QueryResult(Arrays.asList("first_name", "last_name"), "Employees");
        queryResult.addRow(Arrays.asList("first_name", "last_name"), "Employees", Arrays.asList(null, "Doe"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testNullRegExpMatchOperator() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT(name ~ 'John.*')";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Employees");
        queryResult.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }
    @Test
    public void testNullSimilarToExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT(name SIMILAR TO 'John%')";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Employees");
        queryResult.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList(null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

}
