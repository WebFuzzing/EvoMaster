package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnReferenceResolverTest {

    private DbInfoDto schema;
    private ColumnReferenceResolver resolver;

    @BeforeEach
    void setUp() {

        schema = new DbInfoDto();
        TableDto employeesTable = createTableDto("employees");
        employeesTable.columns.add(createColumnDto("id"));
        employeesTable.columns.add(createColumnDto("first_name"));
        employeesTable.columns.add(createColumnDto("department_id"));
        employeesTable.columns.add(createColumnDto("salary"));

        TableDto departmentsTable = createTableDto("departments");
        departmentsTable.columns.add(createColumnDto("id"));
        departmentsTable.columns.add(createColumnDto("department_name"));

        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);

        resolver = new ColumnReferenceResolver(schema);
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
    void testResolveColumnWithExplicitTable() throws Exception {
        String sql = "SELECT e.first_name FROM employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new net.sf.jsqlparser.schema.Table("e"));

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertEquals("employees", ((SqlBaseTableReference) reference.getTableReference()).getFullyQualifiedName());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnWithoutExplicitTable() throws Exception {
        String sql = "SELECT first_name FROM employees";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertEquals("employees", ((SqlBaseTableReference) reference.getTableReference()).getFullyQualifiedName());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInJoin() throws Exception {
        String sql = "SELECT e.first_name, d.department_name FROM employees e JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column columnE = new Column();
        columnE.setColumnName("first_name");
        columnE.setTable(new net.sf.jsqlparser.schema.Table("e"));

        ColumnReference referenceE = resolver.resolveColumnReference(columnE);
        assertNotNull(referenceE);
        assertEquals("employees", ((SqlBaseTableReference) referenceE.getTableReference()).getFullyQualifiedName());
        assertEquals("first_name", referenceE.getColumnName());

        Column columnD = new Column();
        columnD.setColumnName("department_name");
        columnD.setTable(new net.sf.jsqlparser.schema.Table("d"));

        ColumnReference referenceD = resolver.resolveColumnReference(columnD);
        assertNotNull(referenceD);
        assertEquals("departments", ((SqlBaseTableReference) referenceD.getTableReference()).getFullyQualifiedName());
        assertEquals("department_name", referenceD.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInSubquery() throws Exception {
        String sql = "SELECT e.first_name FROM (SELECT * FROM employees) e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new net.sf.jsqlparser.schema.Table("e"));

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("(SELECT * FROM employees) e", ((SqlDerivedTableReference) reference.getTableReference()).getSelect().toString());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }


    @Test
    void testResolveColumnInNestedSubquery() throws Exception {
        String sql = "SELECT subquery1.first_name FROM (SELECT first_name FROM (SELECT * FROM employees) subquery2) subquery1";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new net.sf.jsqlparser.schema.Table("subquery1"));

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInWithClause() throws Exception {
        String sql = "WITH subquery AS (SELECT id, first_name FROM employees) SELECT subquery.first_name FROM subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new net.sf.jsqlparser.schema.Table("subquery"));

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveAmbiguousColumnInJoin() throws Exception {
        String sql = "SELECT id FROM employees e JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("id");

        ColumnReference columnReference = resolver.resolveColumnReference(column);
        assertEquals("id", columnReference.getColumnName());
        assertEquals("employees", ((SqlBaseTableReference) columnReference.getTableReference()).getFullyQualifiedName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInUnion() throws Exception {
        String sql = "SELECT id FROM employees UNION SELECT id FROM departments";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("id");

        ColumnReference columnReference = resolver.resolveColumnReference(column);
        assertEquals("id", columnReference.getColumnName());
        assertEquals("SELECT id FROM employees UNION SELECT id FROM departments", ((SqlDerivedTableReference) columnReference.getTableReference()).getSelect().toString());
        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInComplexJoinWithSubquery() throws Exception {
        String sql = "SELECT e.first_name, subquery.department_name FROM employees e " +
                "JOIN (SELECT id, department_name FROM departments) subquery ON e.department_id = subquery.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column columnE = new Column();
        columnE.setColumnName("first_name");
        columnE.setTable(new net.sf.jsqlparser.schema.Table("e"));

        ColumnReference referenceE = resolver.resolveColumnReference(columnE);
        assertNotNull(referenceE);
        assertEquals("employees", ((SqlBaseTableReference) referenceE.getTableReference()).getFullyQualifiedName());
        assertEquals("first_name", referenceE.getColumnName());

        Column columnSubquery = new Column();
        columnSubquery.setColumnName("department_name");
        columnSubquery.setTable(new net.sf.jsqlparser.schema.Table("subquery"));

        ColumnReference referenceSubquery = resolver.resolveColumnReference(columnSubquery);
        assertNotNull(referenceSubquery);
        assertTrue(referenceSubquery.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("department_name", referenceSubquery.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveAmbiguousColumnInRightJoin() throws Exception {
        String sql = "SELECT department_name FROM employees e JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("department_name");

        ColumnReference columnReference = resolver.resolveColumnReference(column);
        assertEquals("department_name", columnReference.getColumnName());
        assertEquals("departments", ((SqlBaseTableReference) columnReference.getTableReference()).getFullyQualifiedName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveMissingColumn() throws Exception {
        String sql = "SELECT department_address FROM departments";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("department_address");

        assertEquals(null, resolver.resolveColumnReference(column));

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnFromSubquery() throws Exception {
        String sql = "SELECT first_name FROM (SELECT first_name FROM employees)";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        ColumnReference columnReference = resolver.resolveColumnReference(column);
        assertNotNull(columnReference);
        assertTrue(columnReference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("first_name", columnReference.getColumnName());
        assertEquals("(SELECT first_name FROM employees)", ((SqlDerivedTableReference) columnReference.getTableReference()).getSelect().toString());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInJoinWithSubquery() throws Exception {
        String sql = "SELECT employee_id, department_name " +
                "FROM ( " +
                "    SELECT id AS employee_id, department_id " +
                "    FROM employees " +
                "    WHERE salary > 50000 " +
                ") e " +
                "INNER JOIN departments d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        // Resolve employee_id from subquery e
        Column columnEmployeeId = new Column();
        columnEmployeeId.setColumnName("employee_id");

        ColumnReference referenceEmployeeId = resolver.resolveColumnReference(columnEmployeeId);
        assertNotNull(referenceEmployeeId);
        assertTrue(referenceEmployeeId.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("employee_id", referenceEmployeeId.getColumnName());
        assertEquals("(SELECT id AS employee_id, department_id FROM employees WHERE salary > 50000) e",
                ((SqlDerivedTableReference) referenceEmployeeId.getTableReference()).getSelect().toString());

        // Resolve department_name from table d
        Column columnDepartmentName = new Column();
        columnDepartmentName.setColumnName("department_name");

        ColumnReference referenceDepartmentName = resolver.resolveColumnReference(columnDepartmentName);
        assertNotNull(referenceDepartmentName);
        assertEquals("departments", ((SqlBaseTableReference) referenceDepartmentName.getTableReference()).getFullyQualifiedName());
        assertEquals("department_name", referenceDepartmentName.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnInSubqueryWithNoAlias() throws Exception {
        String sql = "SELECT first_name FROM (SELECT * FROM employees)";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("first_name", reference.getColumnName());
        assertEquals("(SELECT * FROM employees)", ((SqlDerivedTableReference) reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void resolveColumnAliasInSubqueryWithWhereClause() throws Exception {
        String sql = "SELECT name, income FROM (SELECT first_name AS name, salary AS income FROM Employees) AS subquery WHERE income > 100";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        // Resolve alias 'name' from subquery
        Column columnName = new Column();
        columnName.setColumnName("name");
        columnName.setTable(new net.sf.jsqlparser.schema.Table("subquery"));

        ColumnReference referenceName = resolver.resolveColumnReference(columnName);
        assertNotNull(referenceName);
        assertTrue(referenceName.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("name", referenceName.getColumnName());
        assertEquals("(SELECT first_name AS name, salary AS income FROM Employees) AS subquery", ((SqlDerivedTableReference) referenceName.getTableReference()).getSelect().toString());

        // Resolve alias 'income' from subquery
        Column columnIncome = new Column();
        columnIncome.setColumnName("income");
        columnIncome.setTable(new net.sf.jsqlparser.schema.Table("subquery"));

        ColumnReference referenceIncome = resolver.resolveColumnReference(columnIncome);
        assertNotNull(referenceIncome);
        assertTrue(referenceIncome.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("income", referenceIncome.getColumnName());
        assertEquals("(SELECT first_name AS name, salary AS income FROM Employees) AS subquery", ((SqlDerivedTableReference) referenceIncome.getTableReference()).getSelect().toString());

        Select view = ((SqlDerivedTableReference) referenceIncome.getTableReference()).getSelect();
        ColumnReference baseTableColumnReference = resolver.findBaseTableColumnReference(view, columnIncome);

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveAllTableColumns() throws Exception {
        String sql = "SELECT first_name  FROM (SELECT employees.* FROM employees) AS e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new net.sf.jsqlparser.schema.Table("e"));

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertEquals("(SELECT employees.* FROM employees) AS e", ((SqlDerivedTableReference) reference.getTableReference()).getSelect().toString());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnWithAlias() throws Exception {
        String sql = "SELECT e.first_name AS fname FROM employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("fname");

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertEquals("employees", ((SqlBaseTableReference) reference.getTableReference()).getFullyQualifiedName());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnAliasInSubquery() throws Exception {
        String sql = "SELECT subquery.fname FROM (SELECT first_name AS fname FROM employees) subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("fname");
        column.setTable(new net.sf.jsqlparser.schema.Table("subquery"));

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("fname", reference.getColumnName());
        assertEquals("(SELECT first_name AS fname FROM employees) subquery", ((SqlDerivedTableReference) reference.getTableReference()).getSelect().toString());
        resolver.exitCurrentSelectContext();
    }

    @Test
    void testResolveColumnAliasInJoin() throws Exception {
        String sql = "SELECT e.fname, d.department_name FROM (SELECT first_name AS fname FROM employees) e " +
                "JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        // Resolve alias fname from subquery e
        Column columnE = new Column();
        columnE.setColumnName("fname");
        columnE.setTable(new net.sf.jsqlparser.schema.Table("e"));

        ColumnReference referenceE = resolver.resolveColumnReference(columnE);
        assertNotNull(referenceE);
        assertTrue(referenceE.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("fname", referenceE.getColumnName());

        // Resolve department_name from table d
        Column columnD = new Column();
        columnD.setColumnName("department_name");
        columnD.setTable(new net.sf.jsqlparser.schema.Table("d"));

        ColumnReference referenceD = resolver.resolveColumnReference(columnD);
        assertNotNull(referenceD);
        assertEquals("departments", ((SqlBaseTableReference) referenceD.getTableReference()).getFullyQualifiedName());
        assertEquals("department_name", referenceD.getColumnName());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testColumnInParenthesizedSelect() throws Exception {
        String sql = "SELECT first_name FROM ((SELECT first_name FROM employees))";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("first_name", reference.getColumnName());
        assertEquals("(SELECT first_name FROM employees)", ((SqlDerivedTableReference)reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testColumnInNestedParenthesizedSelect() throws Exception {
        String sql = "SELECT first_name FROM (((SELECT first_name FROM employees)))";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("first_name", reference.getColumnName());
        assertEquals("(SELECT first_name FROM employees)", ((SqlDerivedTableReference)reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentSelectContext();
    }

    @Test
    void testColumnInSetOperationListWithAlias() throws Exception {
        String sql = "SELECT id AS emp_id FROM employees UNION SELECT id AS dept_id FROM departments";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterSelectContext(select);

        Column column = new Column();
        column.setColumnName("emp_id");

        ColumnReference reference = resolver.resolveColumnReference(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTableReference);
        assertEquals("emp_id", reference.getColumnName());
        assertEquals("SELECT id AS emp_id FROM employees UNION SELECT id AS dept_id FROM departments", ((SqlDerivedTableReference)reference.getTableReference()).getSelect().toString())
        ;

        resolver.exitCurrentSelectContext();
    }
}
