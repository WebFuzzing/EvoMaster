package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
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

import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.TRUE_TRUTHNESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqlHeuristicsCalculatorTest {

    @Test
    public void testSelectFromTableWithRows() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Person");
        queryResult.addRow(new DataRow("name", "John", "Person"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectWithFalseWhereConditionWithoutFrom() {
        String sqlCommand = "SELECT 1 AS example_column WHERE 1 = 0";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, virtualTableContents);

        double hquery = TruthnessUtils.buildAndAggregationTruthness(TRUE_TRUTHNESS, new Truthness(SqlHeuristicsCalculator.C, 1d)).getOfTrue();
        double expectedDistance = 1 - hquery;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testSelectNoFromNeitherWhereClauses() {
        String sqlCommand = "SELECT 1 AS example_column";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        virtualTableContents.addRow(new DataRow("example_column", 1, null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, virtualTableContents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testSelectFromEmptyTable() {
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Person");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, queryResult);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinWithRowsInLeftTable() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");

        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightJoinWithRowsInRightTable() {
        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightJoinWithRowsInLeftTable() {
        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTables = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTables);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinWithRowsInRightTable() {
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinWithRowsInRightTable() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightOuterJoinWithRowsInLeftTable() {
        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinWithRowsInLeftTable() {
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightOuterJoinWithRowsInRightTable() {
        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinWithEmptyTables() {
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinWithRows() {
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testInnerJoin() {
        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testManyInnerJoins() {
        String sqlCommand = "SELECT Employees.name, Departments.department_name, Projects.project_name\n" +
                "FROM Employees\n" +
                "JOIN Departments ON Employees.department_id = Departments.department_id\n" +
                "JOIN Projects ON Employees.project_id = Projects.project_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id", "project_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id", "project_id"), "employees", Arrays.asList("John", 1, 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        QueryResult projects = new QueryResult(Arrays.asList("project_id", "project_name"), "projects");
        projects.addRow(Arrays.asList("project_id", "project_name"), "projects", Arrays.asList(1, "ProjectX"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, employees, departments, projects);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testUnion() {
        String sqlCommand = "SELECT name FROM Employees " +
                "UNION " +
                "SELECT name FROM Departments ";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John"));

        QueryResult departments = new QueryResult(Collections.singletonList("name"), "Departments");
        departments.addRow(Collections.singletonList("name"), "Departments", Collections.singletonList("Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoin() {
        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "CROSS JOIN Departments";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoin() {
        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "LEFT JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightJoin() {
        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "RIGHT JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectFromSubquery() {
        String sqlCommand = "SELECT name FROM (SELECT name FROM Employees) AS Subquery";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(new DataRow("name", "John", "Employees"));

        QueryResult[] arrayOfQueryResultSet = {employees};
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);

        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(null, null,arrayOfQueryResultSet);
        SqlHeuristicResult heuristicResult = calculator.calculateHeuristicQuery(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
    }




}
