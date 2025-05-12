package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.heuristic.SqlColumnReference;
import org.evomaster.client.java.sql.heuristic.SqlBaseTableReference;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TablesAndColumnsFinderTest {

    private static DbInfoDto createSchema() {
        DbInfoDto schema = new DbInfoDto();

        ColumnDto userNameColumn = new ColumnDto();
        userNameColumn.name = "name";
        userNameColumn.table = "Users";

        ColumnDto ageColumn = new ColumnDto();
        ageColumn.name = "age";
        ageColumn.table = "Users";

        TableDto usersTable = new TableDto();
        usersTable.name = "Users";
        usersTable.columns.add(userNameColumn);
        usersTable.columns.add(ageColumn);

        ColumnDto employeesNameColumn = new ColumnDto();
        employeesNameColumn.name = "name";
        employeesNameColumn.table = "Employees";

        ColumnDto departmentIdColumn = new ColumnDto();
        departmentIdColumn.name = "department_id";
        departmentIdColumn.table = "Employees";

        TableDto employeesTable = new TableDto();
        employeesTable.name = "Employees";
        employeesTable.columns.add(employeesNameColumn);
        employeesTable.columns.add(departmentIdColumn);

        ColumnDto departmentNameColumn = new ColumnDto();
        departmentNameColumn.name = "department_name";
        departmentNameColumn.table = "Departments";

        ColumnDto departmentsIdColumn = new ColumnDto();
        departmentsIdColumn.name = "id";
        departmentsIdColumn.table = "Departments";

        TableDto departmentsTable = new TableDto();
        departmentsTable.name = "Departments";
        departmentsTable.columns.add(departmentsIdColumn);
        departmentsTable.columns.add(departmentNameColumn);

        ColumnDto expiredColumn = new ColumnDto();
        expiredColumn.name = "expired";
        expiredColumn.table = "voting";

        ColumnDto createdAtColumn = new ColumnDto();
        createdAtColumn.name = "created_at";
        createdAtColumn.table = "voting";

        ColumnDto groupIdColumn = new ColumnDto();
        groupIdColumn.name = "group_id";
        groupIdColumn.table = "voting";


        TableDto votingTable = new TableDto();
        votingTable.name = "voting";
        votingTable.columns.add(expiredColumn);
        votingTable.columns.add(createdAtColumn);
        votingTable.columns.add(groupIdColumn);

        ColumnDto groupsIdColumn = new ColumnDto();
        groupsIdColumn.name = "id";
        groupsIdColumn.table = "groups";

        ColumnDto votingDurationColumn = new ColumnDto();
        votingDurationColumn.name = "voting_duration";
        votingDurationColumn.table = "groups";

        TableDto groupsTable = new TableDto();
        groupsTable.name = "groups";
        groupsTable.columns.add(groupsIdColumn);
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
        statement.accept(finder);


        final SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));

    }

    private Set<String> createBooleanConstantNames() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList("true", "false")));
    }

    @Test
    void findsTablesAndColumnsInJoinQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();

        String sql = "SELECT e.name, u.age FROM Employees e JOIN Users u ON e.name = u.name";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(employeesTableReference));
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(1, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));

    }

    @Test
    void findsTablesAndColumnsInSubquery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name FROM (SELECT name, age FROM Users WHERE age > 30) AS subquery";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInUnionQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name FROM Users UNION SELECT name FROM Employees";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);


        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");
        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));
        assertTrue(finder.containsColumnReferences(employeesTableReference));

        assertEquals(1, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));

        assertEquals(1, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));
    }

    @Test
    void handlesQueryWithoutTables() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT 1";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertTrue(finder.getBaseTableReferences().isEmpty());
    }

    @Test
    void findsTablesAndColumnsInNestedSubqueries() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name FROM (SELECT name FROM (SELECT name, age FROM Users WHERE age > 30) AS innerSubquery) AS outerSubquery";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInUnionWithAliases() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT u.name FROM Users u UNION SELECT e.name FROM Employees e";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);


        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");
        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));
        assertTrue(finder.containsColumnReferences(employeesTableReference));

        assertEquals(1, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));

        assertEquals(1, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));
    }

    @Test
    void findsTablesAndColumnsInJoinWithSubqueries() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT e.name, u.age FROM (SELECT name FROM Employees) e JOIN (SELECT age FROM Users) u ON e.name = u.age";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);


        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(employeesTableReference));
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(1, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));

        assertEquals(1, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInComplexQueryWithMultipleJoinsAndSubqueries() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT e.name, d.department_name FROM (SELECT name, department_id FROM Employees) e " +
                "JOIN (SELECT id, department_name FROM Departments) d ON e.department_id = d.id";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference departmentsTableReference = new SqlBaseTableReference("Departments");


        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(employeesTableReference));
        assertTrue(finder.containsColumnReferences(departmentsTableReference));

        assertEquals(2, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "department_id")));

        assertEquals(2, finder.getColumnReferences(departmentsTableReference).size());
        assertTrue(finder.getColumnReferences(departmentsTableReference).contains(new SqlColumnReference(departmentsTableReference, "id")));
        assertTrue(finder.getColumnReferences(departmentsTableReference).contains(new SqlColumnReference(departmentsTableReference, "department_name")));
    }

    @Test
    void findsTablesAndColumnsInCountQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT COUNT(*) AS n FROM Users f WHERE f.age > 18";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
    }

    @Test
    void testCountWithoutWhere() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT COUNT(f.age) AS n FROM Users f";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(1, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInGroupByQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT COUNT(name), age FROM Users u GROUP BY age";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
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
        statement.accept(finder);


        SqlBaseTableReference votingTableReference = new SqlBaseTableReference("voting");
        SqlBaseTableReference groupsTableReference = new SqlBaseTableReference("groups");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(votingTableReference));
        assertTrue(finder.containsColumnReferences(groupsTableReference));

        assertEquals(3, finder.getColumnReferences(votingTableReference).size());
        assertTrue(finder.getColumnReferences(votingTableReference).contains(new SqlColumnReference(votingTableReference, "expired")));
        assertTrue(finder.getColumnReferences(votingTableReference).contains(new SqlColumnReference(votingTableReference, "created_at")));
        assertTrue(finder.getColumnReferences(votingTableReference).contains(new SqlColumnReference(votingTableReference, "group_id")));

        assertEquals(2, finder.getColumnReferences(groupsTableReference).size());
        assertTrue(finder.getColumnReferences(groupsTableReference).contains(new SqlColumnReference(groupsTableReference, "voting_duration")));
        assertTrue(finder.getColumnReferences(groupsTableReference).contains(new SqlColumnReference(groupsTableReference, "id")));
    }

    @Test
    void findsTablesAndColumnsInUpdateStatement() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "UPDATE Users SET age = 30 WHERE name = 'John'";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);


        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
    }

    @Test
    void findsTablesAndColumnsInDeleteStatement() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "DELETE FROM Users WHERE age > 18";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(1, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesInDelete() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "DELETE FROM Users";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.getBaseTableReferences().contains(usersTableReference));

    }

    @Test
    void findsTablesAndColumnsInSelectAllQuery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT * FROM Users";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTablesAndColumnsInSelectAllFromDerivedTable() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT v.* FROM (SELECT name, age FROM Users WHERE age > 18) v";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
    }

    @Test
    void findsTwoColumns() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT * FROM Users WHERE name = 'joh' AND age IS NULL";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(usersTableReference));

        assertEquals(2, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "name")));
    }

    @Test
    void findsDeleteJoinNoOnClause() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "DELETE FROM users JOIN departments WHERE age = 25";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        SqlBaseTableReference usersTableReference = new SqlBaseTableReference("Users");
        SqlBaseTableReference departmentsTableReference = new SqlBaseTableReference("Departments");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.getBaseTableReferences().contains(usersTableReference));
        assertTrue(finder.getBaseTableReferences().contains(departmentsTableReference));

        assertEquals(1, finder.getColumnReferences(usersTableReference).size());
        assertTrue(finder.getColumnReferences(usersTableReference).contains(new SqlColumnReference(usersTableReference, "age")));


    }
}
