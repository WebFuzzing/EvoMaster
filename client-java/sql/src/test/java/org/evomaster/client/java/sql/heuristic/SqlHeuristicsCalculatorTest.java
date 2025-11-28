package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.QueryResultSet;
import org.evomaster.client.java.sql.VariableDescriptor;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.TRUE_TRUTHNESS;
import static org.junit.jupiter.api.Assertions.*;

import net.sf.jsqlparser.schema.Column;


public class SqlHeuristicsCalculatorTest {

    @Test
    public void testSelectFromTableWithRows() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());
        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));
    }

    @Test
    public void testSelectWithFalseWhereConditionWithoutFrom() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 1 AS example_column WHERE 1 = 0";

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        double expectedOfTrue = TruthnessUtils.buildAndAggregationTruthness(TRUE_TRUTHNESS, new Truthness(SqlHeuristicsCalculator.C, 1d)).getOfTrue();
        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("example_column", "example_column", null), queryResult.seeVariableDescriptors().get(0));

    }


    @Test
    public void testSelectNoFromNeitherWhereClauses() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 1 AS example_column";

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();


        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("example_column", "example_column", null), queryResult.seeVariableDescriptors().get(0));
    }


    @Test
    public void testSelectFromEmptyTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM Person";
        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);
        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(false, heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));

        assertEquals(0, queryResult.seeRows().size());
    }

    @Test
    public void testLeftJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
    }


    @Test
    public void testRightJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        /*
         * Since name is both tableA.name and tableB.name exist,
         * the "name" column is resolved as tableA.name instead
         * of tableB.name.
         */
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));

    }

    @Test
    public void testRightJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(false, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testLeftJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA LEFT JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        assertEquals(false, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testLeftOuterJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        double expectedOfTrue = SqlHeuristicsCalculator.C;
        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testRightOuterJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        double expectedOfTrue = SqlHeuristicsCalculator.C;
        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());
    }

    @Test
    public void testLeftOuterJoinWithRowsInLeftTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA LEFT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));

    }


    @Test
    public void testRightOuterJoinWithRowsInRightTable() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM TableA RIGHT OUTER JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        rightTable.addRow(new DataRow("name", "John", "TableB"));
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));


    }

    @Test
    public void testCrossJoinWithEmptyTables() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        QueryResultSet queryResultSet = QueryResultSet.build(leftTable, rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        double expectedOfTrue = SqlHeuristicsCalculator.C;

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(expectedOfTrue, heuristicResult.getTruthness().getOfTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(0, heuristicResult.getQueryResult().seeRows().size());

    }

    @Test
    public void testCrossJoinWithRows() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM TableA CROSS JOIN TableB";
        QueryResult leftTable = new QueryResult(Collections.singletonList("name"), "TableA");
        QueryResult rightTable = new QueryResult(Collections.singletonList("name"), "TableB");
        leftTable.addRow(new DataRow("name", "John", "TableA"));
        rightTable.addRow(new DataRow("name", "Jack", "TableB"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(leftTable);
        queryResultSet.addQueryResult(rightTable);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "tablea"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
    }

    private static ColumnDto createColumnDto(String columnName) {
        ColumnDto column = new ColumnDto();
        column.name = columnName;
        return column;
    }

    private static TableDto createTableDto(String tableName) {
        TableDto table = new TableDto();
        table.id = new TableIdDto();
        table.id.name = tableName;
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

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(departments);
        queryResultSet.addQueryResult(employees);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));
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

        QueryResultSet queryResultSet = QueryResultSet.build(departments, employees, projects);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(3, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));
        assertEquals(new VariableDescriptor("project_name", "project_name", "projects"), heuristicResult.getQueryResult().seeVariableDescriptors().get(2));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));
        assertEquals("ProjectX", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("project_name"));


    }

    @Test
    public void testUnion() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Employees " +
                "UNION " +
                "SELECT department_name AS name FROM Departments ";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John"));

        QueryResult departments = new QueryResult(Collections.singletonList("department_name"), "Departments");
        departments.addRow(Collections.singletonList("department_name"), "Departments", Collections.singletonList("Sales"));

        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

        assertEquals(2, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(1).getValueByName("name"));

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

        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));


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
        personTable.columns.add(createColumnDto("age"));
        personTable.columns.add(createColumnDto("salary"));

        TableDto categoriesTable = createTableDto("Categories");
        categoriesTable.columns.add(createColumnDto("id"));
        categoriesTable.columns.add(createColumnDto("name"));
        categoriesTable.columns.add(createColumnDto("parent_id"));

        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(projectsTable);
        schema.tables.add(tableA);
        schema.tables.add(tableB);
        schema.tables.add(personTable);
        schema.tables.add(categoriesTable);

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

        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));

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

        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals(null, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));

    }

    @Test
    public void testSelectFromSubquery() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "SELECT name FROM (SELECT name FROM Employees) AS Subquery";
        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(new DataRow("name", "John", "Employees"));

        Select parsedSqlCommand = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        QueryResultSet queryResultSet = QueryResultSet.build(employees);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(1, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", null, null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));

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
        variableDescriptors.add(new VariableDescriptor("first_name", "name", "employees"));
        variableDescriptors.add(new VariableDescriptor("salary", "income", "employees"));
        QueryResult employees = new QueryResult(variableDescriptors);
        employees.addRow(new DataRow(variableDescriptors, Arrays.asList("John", 10000)));

        Select parsedSqlCommand = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        QueryResultSet queryResultSet = QueryResultSet.build(employees);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic(parsedSqlCommand);

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("first_name", "name", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("salary", "income", "employees"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

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

        QueryResult employees = new QueryResult(Arrays.asList("first_name", "salary"), "employees");
        employees.addRow(new DataRow("employees", Arrays.asList("first_name", "salary"), Arrays.asList("John", 10000)));

        Select select = (Select) SqlParserUtils.parseSqlCommand(sqlCommand);

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(employees);
        TableColumnResolver tableColumnResolver = new TableColumnResolver(schema);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(tableColumnResolver)
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic(select);

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("income", "income", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());

        Table subqueryTable = new Table();
        subqueryTable.setName("subquery");

        Column nameColumn = new Column();
        nameColumn.setTable(subqueryTable);
        nameColumn.setColumnName("name");

        Column incomeColumn = new Column();
        incomeColumn.setTable(subqueryTable);
        incomeColumn.setColumnName("income");

        tableColumnResolver.enterStatementeContext(select);

        SqlColumnReference nameSqlColumnReference = tableColumnResolver.resolve(nameColumn);
        assertTrue(nameSqlColumnReference.getTableReference() instanceof SqlDerivedTableReference);

        SqlColumnReference incomeSqlColumnReference = tableColumnResolver.resolve(incomeColumn);
        assertTrue(incomeSqlColumnReference.getTableReference() instanceof SqlDerivedTableReference);

        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName(nameSqlColumnReference.getColumnName()));
        assertEquals(10000, heuristicResult.getQueryResult().seeRows().get(0).getValueByName(incomeSqlColumnReference.getColumnName()));

        tableColumnResolver.exitCurrentStatementContext();
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

        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlDistanceWithMetrics distanceWithMetrics = calculator.computeDistance(sqlCommand);
        assertEquals(0.0, distanceWithMetrics.sqlDistance);
    }

    @Test
    public void testUnsupportedQuery() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "create cached local temporary table if not exists HT_feature_constraint (id bigint not null) on commit drop transactional";
        QueryResultSet queryResultSet = new QueryResultSet();

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlDistanceWithMetrics distanceWithMetrics = calculator.computeDistance(sqlCommand);
        assertEquals(Double.MAX_VALUE, distanceWithMetrics.sqlDistance);
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

        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", null), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().size());
        assertEquals("John", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("name"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));

    }


    @Test
    public void testDelete() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "DELETE FROM departments WHERE department_id=2";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(2, "Marketing"));

        QueryResultSet queryResultSet = QueryResultSet.build(departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Delete) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("department_id", "department_id", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));


    }

    @Test
    public void testUpdate() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "UPDATE departments SET department_name='Telemarketing' WHERE department_name='Marketing'";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(2, "Marketing"));

        QueryResultSet queryResultSet = QueryResultSet.build(departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Update) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());
        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("department_id", "department_id", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));
    }

    @Test
    public void testFailOnInsert() {
        DbInfoDto schema = buildSchema();

        final String sqlCommand = "INSERT INTO departments (department_id,department_name) VALUES (3,'Telemarketing')";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(2, "Marketing"));

        QueryResultSet queryResultSet = QueryResultSet.build(departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlDistanceWithMetrics distanceWithMetrics = calculator.computeDistance(sqlCommand);
        assertEquals(Double.MAX_VALUE, distanceWithMetrics.sqlDistance);

    }

    @Test
    public void testDeleteNoWhere() {
        DbInfoDto schema = buildSchema();

        String sqlCommand = "DELETE FROM departments";

        QueryResult departments = new QueryResult(Arrays.asList("department_id", "department_name"), "departments");
        departments.addRow(Arrays.asList("department_id", "department_name"), "departments", Arrays.asList(1, "Sales"));

        QueryResultSet queryResultSet = QueryResultSet.build(departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Delete) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        assertEquals(2, heuristicResult.getQueryResult().seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("department_id", "department_id", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(0));
        assertEquals(new VariableDescriptor("department_name", "department_name", "departments"), heuristicResult.getQueryResult().seeVariableDescriptors().get(1));

        assertEquals(1, heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_id"));
        assertEquals("Sales", heuristicResult.getQueryResult().seeRows().get(0).getValueByName("department_name"));
    }

    @Test
    public void testSelectNoFromNoWhere() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 24";


        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertEquals(true, heuristicResult.getTruthness().isTrue());
        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor(null, null, null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(1, queryResult.seeRows().size());
        assertEquals(24, ((Number) queryResult.seeRows().get(0).getValueByName(null)).intValue());

    }

    @Test
    public void testUnionSelectNoFromNoWhere() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT 24 UNION SELECT 42";


        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));
        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor(null, null, null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(2, queryResult.seeRows().size());
        assertEquals(24, ((Number) queryResult.seeRows().get(0).getValueByName(null)).intValue());
        assertEquals(42, ((Number) queryResult.seeRows().get(1).getValueByName(null)).intValue());
    }

    @Test
    public void testSelectWithLimit() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person LIMIT 2";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "Joe", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));

        assertEquals(2, queryResult.seeRows().size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Jane", queryResult.seeRows().get(1).getValueByName("name"));
    }

    @Test
    public void testUnionWithLimit() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "(SELECT name FROM Employees) UNION (SELECT department_name AS name FROM Departments) LIMIT 2";

        QueryResult employees = new QueryResult(Collections.singletonList("name"), "Employees");
        employees.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("John"));
        employees.addRow(Collections.singletonList("name"), "Employees", Collections.singletonList("Jane"));


        QueryResult departments = new QueryResult(Collections.singletonList("department_name"), "Departments");
        departments.addRow(Collections.singletonList("department_name"), "Departments", Collections.singletonList("Sales"));
        departments.addRow(Collections.singletonList("department_name"), "Departments", Collections.singletonList("Marketing"));


        QueryResultSet queryResultSet = QueryResultSet.build(employees, departments);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(2, queryResult.seeRows().size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Jane", queryResult.seeRows().get(1).getValueByName("name"));
    }

    @Test
    public void testParenthesizedSelectWithLimit() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "(SELECT name FROM Person) LIMIT 2";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "Joe", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));

        assertEquals(2, queryResult.seeRows().size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Jane", queryResult.seeRows().get(1).getValueByName("name"));
    }

    @Test
    public void testGroupByWithLimit() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT department_id, COUNT(*) FROM Employees GROUP BY department_id LIMIT 1";

        QueryResult employees = new QueryResult(Arrays.asList("name", "department_id"), "Employees");
        employees.addRow(Arrays.asList("name", "department_id"), "Employees", Arrays.asList("John", 1));
        employees.addRow(Arrays.asList("name", "department_id"), "Employees", Arrays.asList("Jane", 1));
        employees.addRow(Arrays.asList("name", "department_id"), "Employees", Arrays.asList("Joe", 2));


        QueryResultSet queryResultSet = QueryResultSet.build(employees);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();

        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.size());
    }

    @Test
    public void testSelectWithLimitZero() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT * FROM Person LIMIT 0";

        QueryResult contents = new QueryResult(Arrays.asList("name", "age"), "Person");
        contents.addRow(new DataRow("Person", Arrays.asList("name", "age"), Arrays.asList("John", 30)));
        contents.addRow(new DataRow("Person", Arrays.asList("name", "age"), Arrays.asList("Jane", 25)));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isFalse());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(0, queryResult.size());
    }

    @Test
    public void testSelectWithOrderByDesc() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person ORDER BY name DESC";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "Joe", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", "name", "person"), queryResult.seeVariableDescriptors().get(0));

        assertEquals(3, queryResult.seeRows().size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Joe", queryResult.seeRows().get(1).getValueByName("name"));
        assertEquals("Jane", queryResult.seeRows().get(2).getValueByName("name"));
    }

    @Test
    public void testSelectWithOrderByAndLimit() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person ORDER BY name DESC LIMIT 1";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "Joe", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
    }

    @Test
    public void testSelectWithOrderByAscending() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person ORDER BY name ASC";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "Joe", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(3, queryResult.size());
        assertEquals("Jane", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Joe", queryResult.seeRows().get(1).getValueByName("name"));
        assertEquals("John", queryResult.seeRows().get(2).getValueByName("name"));
    }

    @Test
    public void testSelectWithOrderByAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person p ORDER BY p.name ASC";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "John", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "Joe", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(3, queryResult.size());
        assertEquals("Jane", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Joe", queryResult.seeRows().get(1).getValueByName("name"));
        assertEquals("John", queryResult.seeRows().get(2).getValueByName("name"));
    }

    @Test
    public void testSelectWithOrderByFunction() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT name FROM Person ORDER BY UPPER(name) ASC";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");
        contents.addRow(new DataRow("name", "john", "Person"));
        contents.addRow(new DataRow("name", "Jane", "Person"));
        contents.addRow(new DataRow("name", "adam", "Person"));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(3, queryResult.size());
        assertEquals("adam", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Jane", queryResult.seeRows().get(1).getValueByName("name"));
        assertEquals("john", queryResult.seeRows().get(2).getValueByName("name"));
    }

    @Test
    public void testWithClauseAndDerivedTable() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "WITH PersonCTE AS (SELECT * FROM Person) SELECT name FROM PersonCTE";

        QueryResult contents = new QueryResult(Arrays.asList("name", "age", "salary"), "Person");
        contents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("John", 30, 50000)));
        contents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("Jane", 25, 60000)));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", null, null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(2, queryResult.seeRows().size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
        assertEquals("Jane", queryResult.seeRows().get(1).getValueByName("name"));
    }


    @Test
    public void testWithClauseAndDerivedTableWithAlias() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "WITH PersonCTE AS (SELECT * FROM Person) SELECT name FROM PersonCTE pCTE WHERE pCTE.name='John'";

        QueryResult contents = new QueryResult(Arrays.asList("name", "age", "salary"), "Person");
        contents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("John", 30, 50000)));
        contents.addRow(new DataRow("Person", Arrays.asList("name", "age", "salary"), Arrays.asList("Jane", 25, 60000)));

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isTrue());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(1, queryResult.seeVariableDescriptors().size());
        assertEquals(new VariableDescriptor("name", null, null), queryResult.seeVariableDescriptors().get(0));

        assertEquals(1, queryResult.seeRows().size());
        assertEquals("John", queryResult.seeRows().get(0).getValueByName("name"));
    }

    @Test
    public void testSelectWithParenthesisSelectItem() {
        DbInfoDto schema = buildSchema();
        String sqlCommand = "SELECT (p.name) FROM Person p";

        QueryResult contents = new QueryResult(Collections.singletonList("name"), "Person");

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(contents);

        SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder builder = new SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder();
        SqlHeuristicsCalculator calculator = builder.withSourceQueryResultSet(queryResultSet)
                .withTableColumnResolver(new TableColumnResolver(schema))
                .build();
        SqlHeuristicResult heuristicResult = calculator.computeHeuristic((Select) SqlParserUtils.parseSqlCommand(sqlCommand));

        assertTrue(heuristicResult.getTruthness().isFalse());

        QueryResult queryResult = heuristicResult.getQueryResult();
        assertEquals(0, queryResult.size());

    }

}
