package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SqlHandlerTest {

    @Test
    public void testPatioIssue() throws Exception {

        String select = "SELECT v.* FROM voting v, groups g WHERE v.expired = false AND '2021-04-28T16:02:27.426+0200' >= v.created_at + g.voting_duration * INTERVAL '1 hour' AND v.group_id = g.id";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<SqlTableId, Set<SqlColumnId>> columnsInvolvedInWhere = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columnsInvolvedInWhere.values().stream().flatMap(s -> s.stream()).noneMatch(c -> c.getColumnId().equals("false")));

        //TODO add more check on returned columns
    }

    @Test
    public void testBooleans() throws Exception {

        String select = "SELECT f.* FROM Foo WHERE f.a = TRUE AND f.b = On AND f.c = false AND f.d = f";

        Statement stmt = CCJSqlParserUtil.parse(select);

        /*
            TODO in the future, when handle boolean constants in parser, this ll need to be updated
         */
        Map<SqlTableId, Set<SqlColumnId>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }

    @Test
    public void testCreateCachedLocalTemporaryTable() {
        String createSql = "create cached local temporary table if not exists HT_feature_constraint (id bigint not null) on commit drop transactional";
        boolean canParseSqlStatement = SqlParserUtils.canParseSqlStatement(createSql);
        assertFalse(canParseSqlStatement);
    }

    @Test
    public void testNoWhere() throws Exception {

        String select = "SELECT * FROM Table v";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<SqlTableId, Set<SqlColumnId>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }


    @Test
    public void testNoFromNoWhere() throws Exception {

        String select = "SELECT 1";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<SqlTableId, Set<SqlColumnId>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }


    @Test
    public void testWhereButNoColumns() throws Exception {

        String select = "SELECT * FROM table WHERE 1 = 1";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<SqlTableId, Set<SqlColumnId>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }

    @Test
    public void testCompleteSqlDistancesInSimpleSelect() throws Exception {
        TaintHandler mockTaintHandler = mock(TaintHandler.class);

        // Create mocked connection (and result set)
        final Connection mockConnection = createMockConnectionForSimpleSelect();

        // create schema
        final DbInfoDto schema = createSchema();

        // Create SqlHandler instance
        SqlHandler sqlHandler = new SqlHandler(mockTaintHandler);
        sqlHandler.setConnection(mockConnection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(true);

        // Mock SQL execution log
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto();
        sqlExecutionLogDto.sqlCommand = "SELECT name, income FROM Employees WHERE income > 100";
        sqlExecutionLogDto.threwSqlExeception = false;

        // Add the SQL command to the buffered commands
        sqlHandler.handle(sqlExecutionLogDto);

        // Execute getSqlDistances
        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(Collections.emptyList(), true);

        // Assertions
        assertNotNull(distances);
        assertFalse(distances.isEmpty());
        assertEquals(1, distances.size());
        assertEquals("SELECT name, income FROM Employees WHERE income > 100", distances.get(0).sqlCommand);
    }

    @Test
    public void testCompleteSqlDistancesWithSimpleJoin() throws Exception {
        TaintHandler mockTaintHandler = mock(TaintHandler.class);

        // Create mocked connection (and result set)
        final Connection mockConnection = createMockConnectionForSimpleJoin();

        // create schema
        final DbInfoDto schema = createSchema();

        // Create SqlHandler instance
        SqlHandler sqlHandler = new SqlHandler(mockTaintHandler);
        sqlHandler.setConnection(mockConnection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(true);

        // Mock SQL execution log
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto();
        sqlExecutionLogDto.sqlCommand = "SELECT e.name, d.name " +
                "FROM Employees e " +
                "JOIN Departments d " +
                "ON e.department_id = d.id " +
                "WHERE e.income > 100";
        sqlExecutionLogDto.threwSqlExeception = false;

        // Add the SQL command to the buffered commands
        sqlHandler.handle(sqlExecutionLogDto);

        // Execute getSqlDistances
        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(Collections.emptyList(), true);

        // Assertions
        assertNotNull(distances);
        assertFalse(distances.isEmpty());
        assertEquals(1, distances.size());
        assertEquals("SELECT e.name, d.name " +
                "FROM Employees e JOIN Departments d ON e.department_id = d.id WHERE e.income > 100", distances.get(0).sqlCommand);
    }

    private static ColumnDto createColumnDto(String tableName, String columnName, String typeName) {
        ColumnDto columnDto = new ColumnDto();
        columnDto.table = tableName;
        columnDto.name = columnName;
        columnDto.type = typeName;
        return columnDto;
    }

    private static @NotNull DbInfoDto createSchema() {
        TableDto employeesTable = new TableDto();
        employeesTable.name = "Employees";
        employeesTable.columns.add(createColumnDto("employees", "id", "INTEGER"));
        employeesTable.columns.add(createColumnDto("employees", "name", "VARCHAR"));
        employeesTable.columns.add(createColumnDto("employees", "income", "INTEGER"));
        employeesTable.columns.add(createColumnDto("employees", "department_id", "INTEGER"));

        TableDto departmentsTable = new TableDto();
        departmentsTable.name = "Departments";
        departmentsTable.columns.add(createColumnDto("departments", "id", "INTEGER"));
        departmentsTable.columns.add(createColumnDto("departments", "name", "VARCHAR"));
        departmentsTable.columns.add(createColumnDto("departments", "location_id", "INTEGER"));

        TableDto locationsTable = new TableDto();
        locationsTable.name = "Locations";
        locationsTable.columns.add(createColumnDto("locations", "id", "INTEGER"));
        locationsTable.columns.add(createColumnDto("locations", "city", "VARCHAR"));

        DbInfoDto schema = new DbInfoDto();
        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(locationsTable);

        return schema;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testCallNextValue(boolean isCompleteSqlHeuristics) {

        String sqlCommand = "CALL next value for hibernate_sequence";
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setCalculateHeuristics(true);
        sqlHandler.setCompleteSqlHeuristics(isCompleteSqlHeuristics);

        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto();
        sqlExecutionLogDto.sqlCommand = sqlCommand;
        sqlExecutionLogDto.threwSqlExeception = false;

        sqlHandler.handle(sqlExecutionLogDto);
    }

    private static @NotNull Connection createMockConnectionForSimpleSelect() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        ResultSet mockEmployeeResultSet = mock(ResultSet.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSetMetaData mockEmployeeMetaData = mock(ResultSetMetaData.class);

        // Mock the behavior: execute query and pretend it has two rows
        when(mockConnection.createStatement()).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getResultSet()).thenReturn(mockEmployeeResultSet);

        when(mockEmployeeResultSet.next()).thenReturn(true, true, false);
        when(mockEmployeeResultSet.getObject(1)).thenReturn("Alice", "Bob");
        when(mockEmployeeResultSet.getObject(2)).thenReturn(35, 99);
        when(mockEmployeeResultSet.getMetaData()).thenReturn(mockEmployeeMetaData);
        when(mockEmployeeMetaData.getColumnCount()).thenReturn(2);
        when(mockEmployeeMetaData.getColumnName(1)).thenReturn("name");
        when(mockEmployeeMetaData.getColumnName(2)).thenReturn("income");
        when(mockEmployeeMetaData.getTableName(1)).thenReturn("Employees");
        when(mockEmployeeMetaData.getTableName(2)).thenReturn("Employees");

        return mockConnection;
    }

    private static @NotNull Connection createMockConnectionForSimpleJoin() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatementForDepartments = mock(PreparedStatement.class);
        PreparedStatement mockPreparedStatementForEmployees = mock(PreparedStatement.class);
        ResultSet mockDepartmentsResultSet = mock(ResultSet.class);
        ResultSet mockEmployeeResultSet = mock(ResultSet.class);
        ResultSetMetaData mockEmployeeMetaData = mock(ResultSetMetaData.class);
        ResultSetMetaData mockDepartmentsMetaData = mock(ResultSetMetaData.class);

        // Mock the behavior: execute query and pretend it has two rows
        when(mockConnection.createStatement()).thenReturn(mockPreparedStatementForDepartments, mockPreparedStatementForEmployees);
        when(mockPreparedStatementForDepartments.getResultSet()).thenReturn(mockDepartmentsResultSet);
        when(mockPreparedStatementForEmployees.getResultSet()).thenReturn(mockEmployeeResultSet);

        // SELECT id, name FROM departments
        when(mockDepartmentsResultSet.next()).thenReturn(true, true, false);
        when(mockDepartmentsResultSet.getObject(1)).thenReturn(1, 2);
        when(mockDepartmentsResultSet.getObject(2)).thenReturn("Sales", "Marketing");
        when(mockDepartmentsResultSet.getMetaData()).thenReturn(mockDepartmentsMetaData);
        when(mockDepartmentsMetaData.getColumnCount()).thenReturn(2);
        when(mockDepartmentsMetaData.getColumnName(1)).thenReturn("id");
        when(mockDepartmentsMetaData.getColumnName(2)).thenReturn("name");
        when(mockDepartmentsMetaData.getTableName(1)).thenReturn("Departments");
        when(mockDepartmentsMetaData.getTableName(2)).thenReturn("Departments");

        // SELECT department_id, income, name FROM employees
        when(mockEmployeeResultSet.next()).thenReturn(true, true, false);
        when(mockEmployeeResultSet.getObject(1)).thenReturn(1, 2);
        when(mockEmployeeResultSet.getObject(2)).thenReturn(35, 99);
        when(mockEmployeeResultSet.getObject(3)).thenReturn("Alice", "Bob");
        when(mockEmployeeResultSet.getMetaData()).thenReturn(mockEmployeeMetaData);
        when(mockEmployeeMetaData.getColumnCount()).thenReturn(3);
        when(mockEmployeeMetaData.getColumnName(1)).thenReturn("department_id");
        when(mockEmployeeMetaData.getColumnName(2)).thenReturn("income");
        when(mockEmployeeMetaData.getColumnName(3)).thenReturn("name");
        when(mockEmployeeMetaData.getTableName(1)).thenReturn("Employees");
        when(mockEmployeeMetaData.getTableName(2)).thenReturn("Employees");
        when(mockEmployeeMetaData.getTableName(3)).thenReturn("Employees");

        return mockConnection;
    }

    @Test
    public void testGetSqlDistancesNotQueryingFromDatabase() {
        SqlHandler sqlHandler = new SqlHandler(null);

        Connection mockConnection = mock(Connection.class);

        DbInfoDto schema = createSchema();
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(true);
        sqlHandler.setConnection(mockConnection);

        // Mock SQL execution log
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto();
        sqlExecutionLogDto.sqlCommand = "SELECT e.name, d.name " +
                "FROM Employees e " +
                "JOIN Departments d " +
                "ON e.department_id = d.id " +
                "WHERE e.income > 100";
        sqlExecutionLogDto.threwSqlExeception = false;

        // Add the SQL command to the buffered commands
        sqlHandler.handle(sqlExecutionLogDto);

        List<InsertionDto> insertionDtos = sql().insertInto("employees", 1L)
                .d("id", "1")
                .d("name", "John")
                .d("income", "50000")
                .d("department_id", "2")
                .and()
                .insertInto("employees", 2L)
                .d("id", "2")
                .d("name", "Jack")
                .d("income", "40000")
                .d("department_id", "2")
                .and()
                .insertInto("departments", 3L)
                .d("id", "2")
                .d("name", "Sales")
                .d("location_id", null)
                .dtos();

        final boolean queryFromDatabase = false;
        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(insertionDtos, queryFromDatabase);

        // Assertions
        assertNotNull(distances);
        assertFalse(distances.isEmpty());
        assertEquals(1, distances.size());
        assertEquals("SELECT e.name, d.name " +
                "FROM Employees e JOIN Departments d ON e.department_id = d.id WHERE e.income > 100", distances.get(0).sqlCommand);
        assertEquals(0, distances.get(0).sqlDistanceWithMetrics.sqlDistance);
    }

    @Test
    public void testGetSqlDistancesNotQueryFromDatabaseUsingEmptyTables() {
        SqlHandler sqlHandler = new SqlHandler(null);

        Connection mockConnection = mock(Connection.class);

        DbInfoDto schema = createSchema();
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(true);
        sqlHandler.setConnection(mockConnection);

        // Mock SQL execution log
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto();
        sqlExecutionLogDto.sqlCommand = "SELECT e.name, d.name " +
                "FROM Employees e " +
                "JOIN Departments d " +
                "ON e.department_id = d.id " +
                "WHERE e.income > 100";
        sqlExecutionLogDto.threwSqlExeception = false;

        // Add the SQL command to the buffered commands
        sqlHandler.handle(sqlExecutionLogDto);

        List<InsertionDto> insertionDtos = sql().insertInto("employees", 1L)
                .d("id", "1")
                .d("name", "John")
                .d("income", "50000")
                .d("department_id", "2")
                .and()
                .insertInto("employees", 2L)
                .d("id", "2")
                .d("name", "Jack")
                .d("income", "40000")
                .d("department_id", "2")
                .dtos();

        final boolean queryFromDatabase = false;
        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(insertionDtos, queryFromDatabase);

        // Assertions
        assertNotNull(distances);
        assertFalse(distances.isEmpty());
        assertEquals(1, distances.size());
        assertEquals("SELECT e.name, d.name " +
                "FROM Employees e JOIN Departments d ON e.department_id = d.id WHERE e.income > 100", distances.get(0).sqlCommand);
        assertNotEquals(0, distances.get(0).sqlDistanceWithMetrics.sqlDistance);
    }

    @Test
    public void testGetSqlDistancesNoInsertions() {
        SqlHandler sqlHandler = new SqlHandler(null);

        Connection mockConnection = mock(Connection.class);

        DbInfoDto schema = createSchema();
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(true);
        sqlHandler.setConnection(mockConnection);

        // Mock SQL execution log
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto();
        sqlExecutionLogDto.sqlCommand = "SELECT e.name, d.name " +
                "FROM Employees e " +
                "JOIN Departments d " +
                "ON e.department_id = d.id " +
                "WHERE e.income > 100";
        sqlExecutionLogDto.threwSqlExeception = false;

        // Add the SQL command to the buffered commands
        sqlHandler.handle(sqlExecutionLogDto);

        List<InsertionDto> insertionDtos = Collections.emptyList();

        final boolean queryFromDatabase = false;
        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(insertionDtos, queryFromDatabase);

        // Assertions
        assertNotNull(distances);
        assertFalse(distances.isEmpty());
        assertEquals(1, distances.size());
        assertEquals("SELECT e.name, d.name " +
                "FROM Employees e JOIN Departments d ON e.department_id = d.id WHERE e.income > 100", distances.get(0).sqlCommand);
        assertNotEquals(0, distances.get(0).sqlDistanceWithMetrics.sqlDistance);
    }

}
