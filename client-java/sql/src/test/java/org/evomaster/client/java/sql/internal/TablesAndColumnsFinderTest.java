package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.heuristic.ColumnReference;
import org.evomaster.client.java.sql.heuristic.SqlBaseTableReference;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TablesAndColumnsFinderTest {

    private static DbInfoDto createSchema() {
        DbInfoDto schema = new DbInfoDto();

        ColumnDto nameColumn = new ColumnDto();
        nameColumn.name = "name";

        ColumnDto ageColumn = new ColumnDto();
        ageColumn.name = "age";

        ColumnDto idColumn = new ColumnDto();
        idColumn.name = "id";

        ColumnDto departmentIdColumn = new ColumnDto();
        departmentIdColumn.name = "department_id";

        ColumnDto departmentNameColumn = new ColumnDto();
        departmentNameColumn.name = "department_name";

        ColumnDto expiredColumn = new ColumnDto();
        expiredColumn.name = "expired";

        ColumnDto createdAtColumn = new ColumnDto();
        createdAtColumn.name = "created_at";

        ColumnDto groupIdColumn = new ColumnDto();
        groupIdColumn.name = "group_id";

        ColumnDto votingDurationColumn = new ColumnDto();
        votingDurationColumn.name = "voting_duration";

        TableDto usersTable = new TableDto();
        usersTable.name = "Users";
        usersTable.columns.add(nameColumn);
        usersTable.columns.add(ageColumn);

        TableDto employeesTable = new TableDto();
        employeesTable.name = "Employees";
        employeesTable.columns.add(nameColumn);
        employeesTable.columns.add(departmentIdColumn);

        TableDto departmentsTable = new TableDto();
        departmentsTable.name = "Departments";
        departmentsTable.columns.add(idColumn);
        departmentsTable.columns.add(departmentNameColumn);

        TableDto votingTable = new TableDto();
        votingTable.name = "voting";
        votingTable.columns.add(expiredColumn);
        votingTable.columns.add(createdAtColumn);
        votingTable.columns.add(groupIdColumn);

        TableDto groupsTable = new TableDto();
        groupsTable.name = "groups";
        groupsTable.columns.add(idColumn);
        groupsTable.columns.add(votingDurationColumn);

        schema.tables.add(usersTable);
        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(votingTable);
        schema.tables.add(groupsTable);
        return schema;
    }

    @Test
    void findsTablesAndColumnsInSimpleSelectQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name, age FROM Users WHERE age>18";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));


        final SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(2, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));

    }

    @Test
    void findsTablesAndColumnsInJoinQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT e.name, u.age FROM Employees e JOIN Users u ON e.name = u.name";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Employees"));
        assertTrue(tables.contains("Users"));

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(2, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(employeesTableReference));
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(1, finder.getColumnReferences().get(employeesTableReference).size());
        assertTrue(finder.getColumnReferences().get(employeesTableReference).contains(new ColumnReference(employeesTableReference, "name")));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInSubquery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name FROM (SELECT name, age FROM Users WHERE age > 30) AS subquery";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(2, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInUnionQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name FROM Users UNION SELECT name FROM Employees";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));
        assertTrue(tables.contains("Employees"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");
        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");

        assertEquals(2, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));
        assertTrue(finder.getColumnReferences().keySet().contains(employeesTableReference));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));

        assertEquals(1, finder.getColumnReferences().get(employeesTableReference).size());
        assertTrue(finder.getColumnReferences().get(employeesTableReference).contains(new ColumnReference(employeesTableReference, "name")));
    }

    @Test
    void handlesQueryWithoutTables() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT 1";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.isEmpty());
        assertTrue(finder.getColumnReferences().isEmpty());
    }

    @Test
    void findsTablesAndColumnsInNestedSubqueries() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name FROM (SELECT name FROM (SELECT name, age FROM Users WHERE age > 30) AS innerSubquery) AS outerSubquery";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(2, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInUnionWithAliases() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT u.name FROM Users u UNION SELECT e.name FROM Employees e";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));
        assertTrue(tables.contains("Employees"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");
        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");

        assertEquals(2, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));
        assertTrue(finder.getColumnReferences().keySet().contains(employeesTableReference));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));

        assertEquals(1, finder.getColumnReferences().get(employeesTableReference).size());
        assertTrue(finder.getColumnReferences().get(employeesTableReference).contains(new ColumnReference(employeesTableReference, "name")));
    }

    @Test
    void findsTablesAndColumnsInJoinWithSubqueries() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT e.name, u.age FROM (SELECT name FROM Employees) e JOIN (SELECT age FROM Users) u ON e.name = u.age";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Employees"));
        assertTrue(tables.contains("Users"));

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(2, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(employeesTableReference));
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(1, finder.getColumnReferences().get(employeesTableReference).size());
        assertTrue(finder.getColumnReferences().get(employeesTableReference).contains(new ColumnReference(employeesTableReference, "name")));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInComplexQueryWithMultipleJoinsAndSubqueries() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT e.name, d.department_name FROM (SELECT name, department_id FROM Employees) e " +
                "JOIN (SELECT id, department_name FROM Departments) d ON e.department_id = d.id";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Employees"));
        assertTrue(tables.contains("Departments"));

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference departmentsTableReference = new SqlBaseTableReference("Departments");

        assertEquals(2, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(employeesTableReference));
        assertTrue(finder.getColumnReferences().keySet().contains(departmentsTableReference));

        assertEquals(2, finder.getColumnReferences().get(employeesTableReference).size());
        assertTrue(finder.getColumnReferences().get(employeesTableReference).contains(new ColumnReference(employeesTableReference, "name")));
        assertTrue(finder.getColumnReferences().get(employeesTableReference).contains(new ColumnReference(employeesTableReference, "department_id")));

        assertEquals(2, finder.getColumnReferences().get(departmentsTableReference).size());
        assertTrue(finder.getColumnReferences().get(departmentsTableReference).contains(new ColumnReference(departmentsTableReference, "id")));
        assertTrue(finder.getColumnReferences().get(departmentsTableReference).contains(new ColumnReference(departmentsTableReference, "department_name")));
    }

    @Test
    void findsTablesAndColumnsInCountQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT COUNT(*) AS n FROM Users f WHERE f.age > 18";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void testCountWithoutWhere() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT COUNT(f.age) AS n FROM Users f";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInGroupByQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT COUNT(name), age FROM Users u GROUP BY age";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(2, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }

    @Test
    void testPatioApiIssue() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT v.* FROM voting v, groups g " +
                "WHERE v.expired = false " +
                "AND '2021-04-28T16:02:27.426+0200' >= v.created_at + g.voting_duration * INTERVAL '1 hour' " +
                "AND v.group_id = g.id";

        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("voting"));
        assertTrue(tables.contains("groups"));

        SqlBaseTableReference votingTableReference = new SqlBaseTableReference("voting");
        SqlBaseTableReference groupsTableReference = new SqlBaseTableReference("groups");

        assertEquals(2, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(votingTableReference));
        assertTrue(finder.getColumnReferences().keySet().contains(groupsTableReference));

        assertEquals(3, finder.getColumnReferences().get(votingTableReference).size());
        assertTrue(finder.getColumnReferences().get(votingTableReference).contains(new ColumnReference(votingTableReference, "expired")));
        assertTrue(finder.getColumnReferences().get(votingTableReference).contains(new ColumnReference(votingTableReference, "created_at")));
        assertTrue(finder.getColumnReferences().get(votingTableReference).contains(new ColumnReference(votingTableReference, "group_id")));

        assertEquals(2, finder.getColumnReferences().get(groupsTableReference).size());
        assertTrue(finder.getColumnReferences().get(groupsTableReference).contains(new ColumnReference(groupsTableReference, "voting_duration")));
        assertTrue(finder.getColumnReferences().get(groupsTableReference).contains(new ColumnReference(groupsTableReference, "id")));
    }

    @Test
    void findsTablesAndColumnsInUpdateStatement() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "UPDATE Users SET age = 30 WHERE name = 'John'";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(2, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "name")));
    }

    @Test
    void findsTablesAndColumnsInDeleteStatement() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "DELETE FROM Users WHERE age > 18";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.contains("Users"));

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getColumnReferences().keySet().size());
        assertTrue(finder.getColumnReferences().keySet().contains(usersTableReference));

        assertEquals(1, finder.getColumnReferences().get(usersTableReference).size());
        assertTrue(finder.getColumnReferences().get(usersTableReference).contains(new ColumnReference(usersTableReference, "age")));
    }
}
