package org.evomaster.client.java.sql.internal;

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


}
