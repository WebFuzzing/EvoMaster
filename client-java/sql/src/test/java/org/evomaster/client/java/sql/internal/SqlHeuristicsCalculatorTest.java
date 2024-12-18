package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlHeuristicsCalculatorTest {

    @Test
    public void testNoWhereNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult personContents = new QueryResult(Arrays.asList("name"), "Person");
        personContents.addRow(new DataRow("name","John", "Person"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personContents);
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
        QueryResult personContents = new QueryResult(Arrays.asList("name"), "Person");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personContents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
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
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
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
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, tableAcontents, tableBcontents);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinNoFromTableWithRows() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult tableAcontents = new QueryResult(Arrays.asList("name"), "TableA");
        tableAcontents.addRow(new DataRow("name","John", "TableA"));
        QueryResult tableBcontents = new QueryResult(Arrays.asList("name"), "TableB");
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
    public void testSelectNotEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age=18";
        QueryResult personsContents = new QueryResult(Arrays.asList("name","age"), "Persons");
        personsContents.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 18));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personsContents);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectMinorThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18";
        QueryResult personsContents = new QueryResult(Arrays.asList("name","age"), "Persons");
        personsContents.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personsContents);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness(33.0d, 18.0d).getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectMinorThanEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<=18";
        QueryResult personsContents = new QueryResult(Arrays.asList("name","age"), "Persons");
        personsContents.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personsContents);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness(18.0d, 33.0d).invert().getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectGreaterThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>age";
        QueryResult personsContents = new QueryResult(Arrays.asList("name","age"), "Persons");
        personsContents.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personsContents);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness( 33,18.0d).getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectGreaterThanEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>=age";
        QueryResult personsContents = new QueryResult(Arrays.asList("name","age"), "Persons");
        personsContents.addRow(Arrays.asList("name","age"),"Persons",Arrays.asList("John", 33));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, personsContents);

        double equalityTruthness = TruthnessUtils.getLessThanTruthness( 18,33.0d).invert().getOfTrue();
        double scaledTruthnessBetter = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, equalityTruthness).getOfTrue();
        double scaledTruthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C, scaledTruthnessBetter).getOfTrue();
        double expectedDistance = 1 - scaledTruthness;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

}
