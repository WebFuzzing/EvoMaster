package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.VariableDescriptor;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.TRUE_TRUTHNESS;

import net.sf.jsqlparser.schema.Column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqlHeuristicsCalculatorTest {

    @Test
    public void testSelectFromTableWithRows() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Person");
        queryResult.addRow(new DataRow("name", "John", "Person"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, queryResult);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectWithFalseWhereConditionWithoutFrom() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 1 AS example_column WHERE 1 = 0";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, virtualTableContents);

        double hquery = TruthnessUtils.buildAndAggregationTruthness(TRUE_TRUTHNESS, new Truthness(SqlHeuristicsCalculator.C, 1d)).getOfTrue();
        double expectedDistance = 1 - hquery;

        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testSelectNoFromNeitherWhereClauses() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 1 AS example_column";
        QueryResult virtualTableContents = new QueryResult(Collections.singletonList("example_column"), null);
        virtualTableContents.addRow(new DataRow("example_column", 1, null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, virtualTableContents);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testSelectFromEmptyTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM Person";
        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Person");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, queryResult);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");

        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTables = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTables);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightOuterJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testLeftOuterJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }


    @Test
    public void testRightOuterJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinWithEmptyTables() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        double expectedDistance = 1 - SqlHeuristicsCalculator.C;
        assertEquals(expectedDistance, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoinWithRows() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, leftTable, rightTable);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }

    private static ColumnDto createColumnDto(String columnName) {
        ColumnDto column = new ColumnDto();
        column.name = columnName;
        return column;
    }

    private static TableDto createTableDto(String tableName) {
        TableDto table = new TableDto();
        table.name = tableName;
        return table;
    }

    @Test
    public void testInnerJoin() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testManyInnerJoins() {
        DbInfoDto schema = buildSchema();

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

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments, projects);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testUnion() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Employees " +
                "UNION " +
                "SELECT name FROM Departments ";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John"));

        QueryResult departments = new QueryResult(Collections.singletonList("name"), "Departments");
        departments.addRow(Collections.singletonList("name"), "Departments", Collections.singletonList("Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testCrossJoin() {
        final DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "CROSS JOIN Departments";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    private static @NotNull DbInfoDto buildSchema() {
        DbInfoDto schema = new DbInfoDto();

        TableDto employeesTable = createTableDto("Employees");
        employeesTable.columns.add(createColumnDto("name"));
        employeesTable.columns.add(createColumnDto("first_name"));
        employeesTable.columns.add(createColumnDto("department_id"));
        employeesTable.columns.add(createColumnDto("project_id"));
        employeesTable.columns.add(createColumnDto("salary"));

        TableDto departmentsTable = createTableDto("Departments");
        departmentsTable.columns.add(createColumnDto("department_id"));
        departmentsTable.columns.add(createColumnDto("department_name"));

        TableDto projectsTable = createTableDto("Projects");
        projectsTable.columns.add(createColumnDto("project_id"));
        projectsTable.columns.add(createColumnDto("project_name"));


        TableDto tableA = createTableDto("TableA");
        tableA.columns.add(createColumnDto("name"));

        TableDto tableB = createTableDto("TableB");
        tableA.columns.add(createColumnDto("name"));

        TableDto personTable = createTableDto("Person");
        personTable.columns.add(createColumnDto("name"));

        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(projectsTable);
        schema.tables.add(tableA);
        schema.tables.add(tableB);
        schema.tables.add(personTable);
        return schema;
    }

    @Test
    public void testLeftJoin() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "LEFT JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testRightJoin() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT Employees.name, Departments.department_name\n" +
                "FROM Employees\n" +
                "RIGHT JOIN Departments ON Employees.department_id = Departments.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testSelectFromSubquery() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM (SELECT name FROM Employees) AS Subquery";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(new DataRow("name", "John", "Employees"));

        QueryResult[] arrayOfQueryResultSet = {employees};
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);

        TableColumnResolver columnReferenceResolver = new TableColumnResolver(schema);
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(columnReferenceResolver, null, arrayOfQueryResultSet);
        SqlHeuristicResult heuristicResult = calculator.calculateHeuristicQuery(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
    }


    @Test
    public void testSelectWithAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT first_name AS name, salary AS income\n" +
                "    FROM Employees" +
                "    WHERE income > 100";

        List<VariableDescriptor> variableDescriptors = new ArrayList<>();
        variableDescriptors.add(new VariableDescriptor("first_name", "name", "Employees"));
        variableDescriptors.add(new VariableDescriptor("salary", "income", "Employees"));
        QueryResult employees = new QueryResult(variableDescriptors);
        employees.addRow(new DataRow(variableDescriptors, Arrays.asList("John", 10000)));

        QueryResult[] arrayOfQueryResultSet = {employees};
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);

        TableColumnResolver columnReferenceResolver = new TableColumnResolver(schema);
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(columnReferenceResolver, null, arrayOfQueryResultSet);
        SqlHeuristicResult heuristicResult = calculator.calculateHeuristicQuery(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals(10000, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("income"));
    }


    @Test
    public void testSelectFromSubqueryWithAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name, income\n" +
                "FROM (\n" +
                "    SELECT first_name AS name, salary AS income\n" +
                "    FROM Employees\n" +
                ") AS subquery\n" +
                "WHERE income > 100";

        QueryResult employees = new QueryResult(Arrays.asList("first_name", "salary"), "Employees");
        employees.addRow(new DataRow("Employees", Arrays.asList("first_name", "salary"), Arrays.asList("John", 10000)));

        QueryResult[] arrayOfQueryResultSet = {employees};
        Select select = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        TableColumnResolver columnReferenceResolver = new TableColumnResolver(schema);
        SqlHeuristicsCalculator calculator = new SqlHeuristicsCalculator(columnReferenceResolver, null, arrayOfQueryResultSet);
        SqlHeuristicResult heuristicResult = calculator.calculateHeuristicQuery(select);

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());

        Table subqueryTable = new Table();
        subqueryTable.setName("subquery");

        Column nameColumn = new Column();
        nameColumn.setTable(subqueryTable);
        nameColumn.setColumnName("name");

        Column incomeColumn = new Column();
        incomeColumn.setTable(subqueryTable);
        incomeColumn.setColumnName("income");

        columnReferenceResolver.enterStatementeContext(select);

        SqlColumnReference nameSqlColumnReference = columnReferenceResolver.resolve(nameColumn);
        Select nameColumnView = ((SqlDerivedTableReference) nameSqlColumnReference.getTableReference()).getSelect();
        SqlColumnReference nameColumnBaseTableReference = columnReferenceResolver.findBaseTableColumnReference(nameColumnView, nameColumn.getColumnName());

        SqlColumnReference incomeSqlColumnReference = columnReferenceResolver.resolve(incomeColumn);
        Select incomeColumnView = ((SqlDerivedTableReference) incomeSqlColumnReference.getTableReference()).getSelect();
        SqlColumnReference incomeColumnBaseTableReference = columnReferenceResolver.findBaseTableColumnReference(incomeColumnView, incomeColumn.getColumnName());

        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName(nameColumnBaseTableReference.getColumnName()));
        assertEquals(10000, heuristicResult.getQueryResult().seeRows().get(0).getValueByName(incomeColumnBaseTableReference.getColumnName()));

        columnReferenceResolver.exitCurrentStatementContext();
    }

    @Test
    public void testInnerJoinWithAliases() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT e.name, d.department_name\n" +
                "FROM Employees e\n" +
                "JOIN Departments d ON e.department_id = d.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testInnerJoinWithSubqueries() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT e.name, d.department_name\n" +
                "FROM (SELECT name, department_id FROM Employees) e\n" +
                "JOIN (SELECT department_id, department_name FROM Departments) d ON e.department_id = d.department_id";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "employees");
        employees.addRow(Arrays.asList("name", "department_id"), "employees", Arrays.asList("John", 1));

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, schema, null, employees, departments);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

}
