package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SqlHandlerTest {

    @Test
    public void testPatioIssue() throws Exception {

        String select = "SELECT v.* FROM voting v, groups g WHERE v.expired = false AND '2021-04-28T16:02:27.426+0200' >= v.created_at + g.voting_duration * INTERVAL '1 hour' AND v.group_id = g.id";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.values().stream().flatMap(s -> s.stream()).noneMatch(c -> c.equals("false")));

        //TODO add more check on returned columns
    }

    @Test
    public void testBooleans() throws Exception {

        String select = "SELECT f.* FROM Foo WHERE f.a = TRUE AND f.b = On AND f.c = false AND f.d = f";

        Statement stmt = CCJSqlParserUtil.parse(select);

        /*
            TODO in the future, when handle boolean constants in parser, this ll need to be updated
         */
        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
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

        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }


    @Test
    public void testNoFromNoWhere() throws Exception {

        String select = "SELECT 1";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }


    @Test
    public void testWhereButNoColumns() throws Exception {

        String select = "SELECT * FROM table WHERE 1 = 1";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }

    @Test
    public void testGetSqlDistances() throws Exception {
        TaintHandler mockTaintHandler = mock(TaintHandler.class);

        // Create mocked connection (and result set)
        final Connection mockConnection = createMockConnection();

        // create schema
        final DbInfoDto schema = createSchema();

        // Create SqlHandler instance
        SqlHandler sqlHandler = new SqlHandler(mockTaintHandler);
        sqlHandler.setConnection(mockConnection);
        sqlHandler.setSchema(schema);
        sqlHandler.setAdvancedHeuristics(true);

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

    private static @NotNull DbInfoDto createSchema() {
        ColumnDto nameColumn = new ColumnDto();
        nameColumn.name = "name";

        ColumnDto incomeColumn = new ColumnDto();
        incomeColumn.name = "income";

        ColumnDto departmentIdColumn = new ColumnDto();
        departmentIdColumn.name = "id";

        ColumnDto departmentNameColumn = new ColumnDto();
        departmentNameColumn.name = "department_name";

        ColumnDto locationIdColumn = new ColumnDto();
        locationIdColumn.name = "id";

        ColumnDto cityColumn = new ColumnDto();
        cityColumn.name = "city";


        TableDto employeesTable = new TableDto();
        employeesTable.name = "Employees";
        employeesTable.columns.add(nameColumn);
        employeesTable.columns.add(incomeColumn);

        TableDto departmentsTable = new TableDto();
        departmentsTable.name = "Departments";
        departmentsTable.columns.add(departmentIdColumn);
        departmentsTable.columns.add(departmentNameColumn);

        TableDto locationsTable = new TableDto();
        locationsTable.name = "Locations";
        locationsTable.columns.add(locationIdColumn);
        locationsTable.columns.add(cityColumn);

        DbInfoDto schema = new DbInfoDto();
        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(locationsTable);

        return schema;
    }

    private static @NotNull Connection createMockConnection() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        ResultSet mockEmployeeResultSet = mock(ResultSet.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSetMetaData mockEmployeeMetaData = mock(ResultSetMetaData.class);

        // Mock the behavior: execute query and pretend it has two rows
        when(mockConnection.createStatement()).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.execute("SELECT name, income FROM Employees")).thenReturn(true);
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



}
