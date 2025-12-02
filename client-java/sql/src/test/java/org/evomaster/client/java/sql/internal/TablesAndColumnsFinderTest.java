package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto;
import org.evomaster.client.java.sql.heuristic.SqlColumnReference;
import org.evomaster.client.java.sql.heuristic.SqlBaseTableReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TablesAndColumnsFinderTest {

    private static DbInfoDto createSchema() {
        DbInfoDto schema = new DbInfoDto();

        TableDto usersTable = new TableDto();
        usersTable.id = new TableIdDto();
        usersTable.id.name = "Users";
        usersTable.columns.add(createColumnDto("name", "Users"));
        usersTable.columns.add(createColumnDto("age", "Users"));

        TableDto employeesTable = new TableDto();
        employeesTable.id = new TableIdDto();
        employeesTable.id.name = "Employees";
        employeesTable.columns.add(createColumnDto("name", "Employees"));
        employeesTable.columns.add(createColumnDto("department_id", "Employees"));

        TableDto departmentsTable = new TableDto();
        departmentsTable.id = new TableIdDto();
        departmentsTable.id.name = "Departments";
        departmentsTable.columns.add(createColumnDto("id", "Departments"));
        departmentsTable.columns.add(createColumnDto("department_name", "Departments"));


        TableDto votingTable = new TableDto();
        votingTable.id = new TableIdDto();
        votingTable.id.name = "voting";
        votingTable.columns.add(createColumnDto("expired", "voting"));
        votingTable.columns.add(createColumnDto("created_at", "voting"));
        votingTable.columns.add(createColumnDto("group_id", "voting"));

        TableDto groupsTable = new TableDto();
        groupsTable.id = new TableIdDto();
        groupsTable.id.name = "groups";
        groupsTable.columns.add(createColumnDto("id", "groups"));
        groupsTable.columns.add(createColumnDto("voting_duration", "groups"));

        TableDto dbBaseTable = new TableDto();
        dbBaseTable.id = new TableIdDto();
        dbBaseTable.id.name = "db_base";
        dbBaseTable.columns.add(createColumnDto("id", "db_base"));
        dbBaseTable.columns.add(createColumnDto("name", "db_base"));

        TableDto agentsTable = new TableDto();
        agentsTable.id = new TableIdDto();
        agentsTable.id.schema = "public";
        agentsTable.id.name = "agents";
        agentsTable.columns.add(createColumnDto("name", "agents"));
        agentsTable.columns.add(createColumnDto("age", "agents"));


        schema.tables.add(usersTable);
        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(votingTable);
        schema.tables.add(groupsTable);
        schema.tables.add(dbBaseTable);
        schema.tables.add(agentsTable);
        return schema;
    }

    private static @NotNull ColumnDto createColumnDto(String columName, String tableName) {
        ColumnDto employeesNameColumn = new ColumnDto();
        employeesNameColumn.name = columName;
        employeesNameColumn.table = tableName;
        return employeesNameColumn;
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

    @Test
    void findsOuterColumnWithTable() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT * FROM employees WHERE EXISTS (SELECT id FROM departments WHERE employees.department_id = departments.id)";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        assertEquals(2, finder.getBaseTableReferences().size());

        final SqlBaseTableReference employees = new SqlBaseTableReference("employees");
        final SqlBaseTableReference departments = new SqlBaseTableReference("departments");

        assertTrue(finder.getBaseTableReferences().contains(employees));
        assertTrue(finder.getBaseTableReferences().contains(departments));

        assertEquals(2, finder.getColumnReferences(employees).size());
        assertTrue( finder.getColumnReferences(employees).contains(new SqlColumnReference(employees, "name")));
        assertTrue( finder.getColumnReferences(employees).contains(new SqlColumnReference(employees, "department_id")));

        assertEquals(1, finder.getColumnReferences(departments).size());
        assertTrue( finder.getColumnReferences(departments).contains(new SqlColumnReference(departments, "id")));
    }

    @Test
    void findsOuterColumnWithAlias() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT 1 FROM employees e WHERE EXISTS (SELECT id FROM departments d WHERE e.department_id = d.id)";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        assertEquals(2, finder.getBaseTableReferences().size());

        final SqlBaseTableReference employees = new SqlBaseTableReference("employees");
        final SqlBaseTableReference departments = new SqlBaseTableReference("departments");

        assertTrue(finder.getBaseTableReferences().contains(employees));
        assertTrue(finder.getBaseTableReferences().contains(departments));

        assertEquals(1, finder.getColumnReferences(employees).size());
        assertTrue( finder.getColumnReferences(employees).contains(new SqlColumnReference(employees, "department_id")));

        assertEquals(1, finder.getColumnReferences(departments).size());
        assertTrue( finder.getColumnReferences(departments).contains(new SqlColumnReference(departments, "id")));

    }

    @Test
    void findsOuterColumn() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT 1 FROM employees WHERE EXISTS (SELECT id FROM departments WHERE department_id = id)";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);

        statement.accept(finder);

        assertEquals(2, finder.getBaseTableReferences().size());

        final SqlBaseTableReference employees = new SqlBaseTableReference("employees");
        final SqlBaseTableReference departments = new SqlBaseTableReference("departments");

        assertTrue(finder.getBaseTableReferences().contains(employees));
        assertTrue(finder.getBaseTableReferences().contains(departments));

        assertEquals(1, finder.getColumnReferences(employees).size());
        assertTrue( finder.getColumnReferences(employees).contains(new SqlColumnReference(employees, "department_id")));

        assertEquals(1, finder.getColumnReferences(departments).size());
        assertTrue( finder.getColumnReferences(departments).contains(new SqlColumnReference(departments, "id")));
    }

    @Test
    void testInsertionWithUnicode() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "INSERT INTO db_base (id, name) VALUES (NULL, U & 'uow8J\\0080a88rKn4Y')";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.getBaseTableReferences().contains(new SqlBaseTableReference("db_base")));
        assertEquals(false, finder.hasColumnReferences(new SqlBaseTableReference("db_base")));

    }

    @Test
    void testNullValue() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT NULL AS null_value FROM employees";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.getBaseTableReferences().contains(new SqlBaseTableReference("employees")));
        assertEquals(false, finder.hasColumnReferences(new SqlBaseTableReference("employees")));
    }

    @Test
    void testNullValueInSubquery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT null_value FROM (SELECT NULL AS null_value FROM employees)";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.getBaseTableReferences().contains(new SqlBaseTableReference("employees")));
        assertEquals(false, finder.hasColumnReferences(new SqlBaseTableReference("employees")));
    }

    @Test
    void testLongValue() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT 42 AS non_null_value FROM employees";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.getBaseTableReferences().contains(new SqlBaseTableReference("employees")));
        assertEquals(false, finder.hasColumnReferences(new SqlBaseTableReference("employees")));
    }

    @Test
    void testNullValueInSubqueryNoFrom() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT null_value FROM (SELECT NULL AS null_value)";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertEquals(0, finder.getBaseTableReferences().size());

    }

    @Test
    void testMissingTables() throws JSQLParserException {
        DbInfoDto schema = createSchema();

        Assumptions.assumeTrue(schema.tables.stream()
                .filter(t -> t.id.name.equals("Foo"))
                .count()==0);

        String sql = "SELECT * FROM Foo";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        assertEquals(0, finder.getBaseTableReferences().size());

    }

    @Test
    void testDeleteFromWithSubquery() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "DELETE FROM Employees WHERE department_id IN (SELECT id FROM Departments)";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");
        SqlBaseTableReference departmentsTableReference = new SqlBaseTableReference("Departments");

        assertEquals(2, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(employeesTableReference));
        assertTrue(finder.containsColumnReferences(departmentsTableReference));

        assertEquals(1, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "department_id")));

        assertEquals(1, finder.getColumnReferences(departmentsTableReference).size());
        assertTrue(finder.getColumnReferences(departmentsTableReference).contains(new SqlColumnReference(departmentsTableReference, "id")));
    }

    @Test
    void testCommonTableExpression() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "WITH EmployeeCTE AS (SELECT name, department_id FROM Employees WHERE department_id > 0) " +
                "SELECT name FROM EmployeeCTE";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(employeesTableReference));

        assertEquals(2, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "department_id")));
    }


    @Test
    void testCommonTableExpressionWithWhere() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "WITH EmployeeCTE AS (SELECT name, department_id FROM Employees WHERE department_id > 0) " +
                "SELECT name FROM EmployeeCTE empCTE WHERE empCTE.name='John'";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference employeesTableReference = new SqlBaseTableReference("Employees");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(employeesTableReference));

        assertEquals(2, finder.getColumnReferences(employeesTableReference).size());
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "name")));
        assertTrue(finder.getColumnReferences(employeesTableReference).contains(new SqlColumnReference(employeesTableReference, "department_id")));
    }

    @Test
    void testSelectWithExplicitSchema() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT name, age FROM public.agents WHERE age > 18";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference publicUsersTableReference = new SqlBaseTableReference(null, "public","agents");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(publicUsersTableReference));

        assertEquals(2, finder.getColumnReferences(publicUsersTableReference).size());
        assertTrue(finder.getColumnReferences(publicUsersTableReference).contains(new SqlColumnReference(publicUsersTableReference, "name")));
        assertTrue(finder.getColumnReferences(publicUsersTableReference).contains(new SqlColumnReference(publicUsersTableReference, "age")));
    }

    @Test
    void testSelectAllWithExplicitSchema() throws JSQLParserException {
        DbInfoDto schema = createSchema();
        String sql = "SELECT * FROM public.agents";
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(schema);
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(finder);

        SqlBaseTableReference publicUsersTableReference = new SqlBaseTableReference(null, "public","agents");

        assertEquals(1, finder.getBaseTableReferences().size());
        assertTrue(finder.containsColumnReferences(publicUsersTableReference));

        assertEquals(2, finder.getColumnReferences(publicUsersTableReference).size());
        assertTrue(finder.getColumnReferences(publicUsersTableReference).contains(new SqlColumnReference(publicUsersTableReference, "name")));
        assertTrue(finder.getColumnReferences(publicUsersTableReference).contains(new SqlColumnReference(publicUsersTableReference, "age")));
    }


}
