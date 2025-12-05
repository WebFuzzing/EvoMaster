package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.SqlTableId;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableColumnResolverTest {

    private DbInfoDto schema;
    private TableColumnResolver resolver;

    @BeforeEach
    void setUp() {

        schema = new DbInfoDto();
        TableDto employeesTable = createTableDto(null, "employees");
        employeesTable.columns.add(createColumnDto("employees", "id"));
        employeesTable.columns.add(createColumnDto("employees", "first_name"));
        employeesTable.columns.add(createColumnDto("employees", "department_id"));
        employeesTable.columns.add(createColumnDto("employees", "salary"));

        TableDto departmentsTable = createTableDto(null, "departments");
        departmentsTable.columns.add(createColumnDto("departments", "id"));
        departmentsTable.columns.add(createColumnDto("departments", "department_name"));

        TableDto ordersTable = createTableDto(null, "orders");
        ordersTable.columns.add(createColumnDto("orders", "order_id"));
        ordersTable.columns.add(createColumnDto("orders", "order_date"));
        ordersTable.columns.add(createColumnDto("orders", "customer_id"));

        TableDto customersTable = createTableDto(null, "customers");
        customersTable.columns.add(createColumnDto("customers", "customer_id"));
        customersTable.columns.add(createColumnDto("customers", "customer_name"));

        TableDto usersTable = createTableDto("public", "users");
        usersTable.columns.add(createColumnDto("users", "user_id"));
        usersTable.columns.add(createColumnDto("users", "user_name"));


        schema.tables.add(employeesTable);
        schema.tables.add(departmentsTable);
        schema.tables.add(ordersTable);
        schema.tables.add(customersTable);
        schema.tables.add(usersTable);

        resolver = new TableColumnResolver(schema);
    }

    private static ColumnDto createColumnDto(String tableName, String columnName) {
        ColumnDto column = new ColumnDto();
        column.name = columnName;
        column.table = tableName;
        return column;
    }

    private static TableDto createTableDto(String schemaName, String tableName) {
        TableDto table = new TableDto();
        table.id = new TableIdDto();
        table.id.schema = schemaName;
        table.id.name = tableName;
        return table;
    }

    @Test
    void testResolveColumnWithExplicitTable() throws Exception {
        String sql = "SELECT e.first_name FROM employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new Table("e"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) reference.getTableReference()).getTableId());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnWithoutExplicitTable() throws Exception {
        String sql = "SELECT first_name FROM employees";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) reference.getTableReference()).getTableId());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInJoin() throws Exception {
        String sql = "SELECT e.first_name, d.department_name FROM employees e JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column columnE = new Column();
        columnE.setColumnName("first_name");
        columnE.setTable(new Table("e"));

        SqlColumnReference referenceE = resolver.resolve(columnE);
        assertNotNull(referenceE);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) referenceE.getTableReference()).getTableId());
        assertEquals("first_name", referenceE.getColumnName());

        Column columnD = new Column();
        columnD.setColumnName("department_name");
        columnD.setTable(new Table("d"));

        SqlColumnReference referenceD = resolver.resolve(columnD);
        assertNotNull(referenceD);
        assertEquals(new SqlTableId(null,null,"departments"), ((SqlBaseTableReference) referenceD.getTableReference()).getTableId());
        assertEquals("department_name", referenceD.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInSubquery() throws Exception {
        String sql = "SELECT e.first_name FROM (SELECT * FROM employees) e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new Table("e"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("(SELECT * FROM employees) e", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }


    @Test
    void testResolveColumnInNestedSubquery() throws Exception {
        String sql = "SELECT subquery1.first_name FROM (SELECT first_name FROM (SELECT * FROM employees) subquery2) subquery1";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new Table("subquery1"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInWithClause() throws Exception {
        String sql = "WITH subquery AS (SELECT id, first_name FROM employees) SELECT subquery.first_name FROM subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new Table("subquery"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveAmbiguousColumnInJoin() throws Exception {
        String sql = "SELECT id FROM employees e JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("id");

        SqlColumnReference sqlColumnReference = resolver.resolve(column);
        assertEquals("id", sqlColumnReference.getColumnName());
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) sqlColumnReference.getTableReference()).getTableId());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInUnion() throws Exception {
        String sql = "SELECT id FROM employees UNION SELECT id FROM departments";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("id");

        SqlColumnReference sqlColumnReference = resolver.resolve(column);
        assertEquals("id", sqlColumnReference.getColumnName());
        assertEquals("SELECT id FROM employees UNION SELECT id FROM departments", ((SqlDerivedTable) sqlColumnReference.getTableReference()).getSelect().toString());
        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInComplexJoinWithSubquery() throws Exception {
        String sql = "SELECT e.first_name, subquery.department_name FROM employees e " +
                "JOIN (SELECT id, department_name FROM departments) subquery ON e.department_id = subquery.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column columnE = new Column();
        columnE.setColumnName("first_name");
        columnE.setTable(new Table("e"));

        SqlColumnReference referenceE = resolver.resolve(columnE);
        assertNotNull(referenceE);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) referenceE.getTableReference()).getTableId());
        assertEquals("first_name", referenceE.getColumnName());

        Column columnSubquery = new Column();
        columnSubquery.setColumnName("department_name");
        columnSubquery.setTable(new Table("subquery"));

        SqlColumnReference referenceSubquery = resolver.resolve(columnSubquery);
        assertNotNull(referenceSubquery);
        assertTrue(referenceSubquery.getTableReference() instanceof SqlDerivedTable);
        assertEquals("department_name", referenceSubquery.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveAmbiguousColumnInRightJoin() throws Exception {
        String sql = "SELECT department_name FROM employees e JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("department_name");

        SqlColumnReference sqlColumnReference = resolver.resolve(column);
        assertEquals("department_name", sqlColumnReference.getColumnName());
        assertEquals(new SqlTableId(null,null,"departments"), ((SqlBaseTableReference) sqlColumnReference.getTableReference()).getTableId());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveMissingColumn() throws Exception {
        String sql = "SELECT department_address FROM departments";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("department_address");

        assertEquals(null, resolver.resolve(column));

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnFromSubquery() throws Exception {
        String sql = "SELECT first_name FROM (SELECT first_name FROM employees)";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference sqlColumnReference = resolver.resolve(column);
        assertNotNull(sqlColumnReference);
        assertTrue(sqlColumnReference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", sqlColumnReference.getColumnName());
        assertEquals("(SELECT first_name FROM employees)", ((SqlDerivedTable) sqlColumnReference.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
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
        resolver.enterStatementeContext(select);

        // Resolve employee_id from subquery e
        Column columnEmployeeId = new Column();
        columnEmployeeId.setColumnName("employee_id");

        SqlColumnReference referenceEmployeeId = resolver.resolve(columnEmployeeId);
        assertNotNull(referenceEmployeeId);
        assertTrue(referenceEmployeeId.getTableReference() instanceof SqlDerivedTable);
        assertEquals("employee_id", referenceEmployeeId.getColumnName());
        assertEquals("(SELECT id AS employee_id, department_id FROM employees WHERE salary > 50000) e",
                ((SqlDerivedTable) referenceEmployeeId.getTableReference()).getSelect().toString());

        // Resolve department_name from table d
        Column columnDepartmentName = new Column();
        columnDepartmentName.setColumnName("department_name");

        SqlColumnReference referenceDepartmentName = resolver.resolve(columnDepartmentName);
        assertNotNull(referenceDepartmentName);
        assertEquals(new SqlTableId(null,null,"departments"), ((SqlBaseTableReference) referenceDepartmentName.getTableReference()).getTableId());
        assertEquals("department_name", referenceDepartmentName.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInSubqueryWithNoAlias() throws Exception {
        String sql = "SELECT first_name FROM (SELECT * FROM employees)";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", reference.getColumnName());
        assertEquals("(SELECT * FROM employees)", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void resolveColumnAliasInSubqueryWithWhereClause() throws Exception {
        String sql = "SELECT name, income FROM (SELECT first_name AS name, salary AS income FROM Employees) AS subquery WHERE income > 100";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        // Resolve alias 'name' from subquery
        Column columnName = new Column();
        columnName.setColumnName("name");
        columnName.setTable(new Table("subquery"));

        SqlColumnReference referenceName = resolver.resolve(columnName);
        assertNotNull(referenceName);
        assertTrue(referenceName.getTableReference() instanceof SqlDerivedTable);
        assertEquals("name", referenceName.getColumnName());
        assertEquals("(SELECT first_name AS name, salary AS income FROM Employees) AS subquery", ((SqlDerivedTable) referenceName.getTableReference()).getSelect().toString());

        // Resolve alias 'income' from subquery
        Column columnIncome = new Column();
        columnIncome.setColumnName("income");
        columnIncome.setTable(new Table("subquery"));

        SqlColumnReference referenceIncome = resolver.resolve(columnIncome);
        assertNotNull(referenceIncome);
        assertTrue(referenceIncome.getTableReference() instanceof SqlDerivedTable);
        assertEquals("income", referenceIncome.getColumnName());
        assertEquals("(SELECT first_name AS name, salary AS income FROM Employees) AS subquery", ((SqlDerivedTable) referenceIncome.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveAllTableColumns() throws Exception {
        String sql = "SELECT first_name  FROM (SELECT employees.* FROM employees) AS e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new Table("e"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertEquals("(SELECT employees.* FROM employees) AS e", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnWithAlias() throws Exception {
        String sql = "SELECT e.first_name AS fname FROM employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("fname");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) reference.getTableReference()).getTableId());
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnAliasInSubquery() throws Exception {
        String sql = "SELECT subquery.fname FROM (SELECT first_name AS fname FROM employees) subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("fname");
        column.setTable(new Table("subquery"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("fname", reference.getColumnName());
        assertEquals("(SELECT first_name AS fname FROM employees) subquery", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());
        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnAliasInJoin() throws Exception {
        String sql = "SELECT e.fname, d.department_name FROM (SELECT first_name AS fname FROM employees) e " +
                "JOIN departments d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        // Resolve alias fname from subquery e
        Column columnE = new Column();
        columnE.setColumnName("fname");
        columnE.setTable(new Table("e"));

        SqlColumnReference referenceE = resolver.resolve(columnE);
        assertNotNull(referenceE);
        assertTrue(referenceE.getTableReference() instanceof SqlDerivedTable);
        assertEquals("fname", referenceE.getColumnName());

        // Resolve department_name from table d
        Column columnD = new Column();
        columnD.setColumnName("department_name");
        columnD.setTable(new Table("d"));

        SqlColumnReference referenceD = resolver.resolve(columnD);
        assertNotNull(referenceD);
        assertEquals(new SqlTableId(null,null,"departments"), ((SqlBaseTableReference) referenceD.getTableReference()).getTableId());
        assertEquals("department_name", referenceD.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testColumnInParenthesizedSelect() throws Exception {
        String sql = "SELECT first_name FROM ((SELECT first_name FROM employees))";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", reference.getColumnName());
        assertEquals("(SELECT first_name FROM employees)", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testColumnInNestedParenthesizedSelect() throws Exception {
        String sql = "SELECT first_name FROM (((SELECT first_name FROM employees)))";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", reference.getColumnName());
        assertEquals("(SELECT first_name FROM employees)", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testColumnInSetOperationListWithAlias() throws Exception {
        String sql = "SELECT id AS emp_id FROM employees UNION SELECT id AS dept_id FROM departments";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("emp_id");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("emp_id", reference.getColumnName());
        assertEquals("SELECT id AS emp_id FROM employees UNION SELECT id AS dept_id FROM departments", ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInDeleteStatement() throws Exception {
        String sql = "DELETE FROM employees WHERE department_id = 1";
        Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(delete);

        Column column = new Column();
        column.setColumnName("department_id");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) reference.getTableReference()).getTableId());
        assertEquals("department_id", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInUpdateStatement() throws Exception {
        String sql = "UPDATE employees SET salary = 50000 WHERE department_id = 1";
        Update update = (Update) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(update);

        // Resolve column in SET clause
        Column setColumn = new Column();
        setColumn.setColumnName("salary");

        SqlColumnReference setReference = resolver.resolve(setColumn);
        assertNotNull(setReference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) setReference.getTableReference()).getTableId());
        assertEquals("salary", setReference.getColumnName());

        // Resolve column in WHERE clause
        Column whereColumn = new Column();
        whereColumn.setColumnName("department_id");

        SqlColumnReference whereReference = resolver.resolve(whereColumn);
        assertNotNull(whereReference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) whereReference.getTableReference()).getTableId());
        assertEquals("department_id", whereReference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInDeleteWithSubquery() throws Exception {
        String sql = "DELETE FROM employees WHERE department_id IN (SELECT d.id AS dept_id FROM departments d WHERE d.department_name = 'HR')";
        Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(delete);

        // Resolve column in the main DELETE statement
        Column column = new Column();
        column.setColumnName("department_id");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) reference.getTableReference()).getTableId());
        assertEquals("department_id", reference.getColumnName());

        // Resolve alias 'dept_id' in the subquery
        Column d_dept_id_column = new Column();
        d_dept_id_column.setColumnName("dept_id");
        d_dept_id_column.setTable(new Table("d"));

        assertNull(resolver.resolve(d_dept_id_column));

        // access the subquery in the delete statement and resolve the column reference
        InExpression inExpression = (InExpression) delete.getWhere();
        Select subquery = (Select) inExpression.getRightExpression();
        resolver.enterStatementeContext(subquery);

        // again, accessing d.dept_id in the subquery is incorrect
        assertNull(resolver.resolve(d_dept_id_column));

        // but accessing d.id should be correct
        Column d_id_column = new Column();
        d_id_column.setColumnName("id");
        d_id_column.setTable(new Table("d"));

        SqlColumnReference subqueryReference = resolver.resolve(d_id_column);

        assertNotNull(subqueryReference);
        assertEquals(new SqlTableId(null,null,"departments"), ((SqlBaseTableReference) subqueryReference.getTableReference()).getTableId());
        assertEquals("id", subqueryReference.getColumnName());

        resolver.exitCurrentStatementContext();
        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnsInInnerJoin() throws Exception {
        String sql = "SELECT orders.order_id, customers.customer_name, orders.order_date " +
                "FROM orders " +
                "INNER JOIN customers ON orders.customer_id = customers.customer_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        // Resolve Orders.OrderID
        Column orderIdColumn = new Column();
        orderIdColumn.setColumnName("order_id");
        orderIdColumn.setTable(new Table("orders"));

        SqlColumnReference orderIdReference = resolver.resolve(orderIdColumn);
        assertNotNull(orderIdReference);
        assertEquals(new SqlTableId(null,null,"orders"), ((SqlBaseTableReference) orderIdReference.getTableReference()).getTableId());
        assertEquals("order_id", orderIdReference.getColumnName());

        // Resolve Customers.CustomerName
        Column customerNameColumn = new Column();
        customerNameColumn.setColumnName("customer_name");
        customerNameColumn.setTable(new Table("customers"));

        SqlColumnReference customerNameReference = resolver.resolve(customerNameColumn);
        assertNotNull(customerNameReference);
        assertEquals(new SqlTableId(null,null,"customers"), ((SqlBaseTableReference) customerNameReference.getTableReference()).getTableId());
        assertEquals("customer_name", customerNameReference.getColumnName());

        // Resolve Orders.OrderDate
        Column orderDateColumn = new Column();
        orderDateColumn.setColumnName("order_date");
        orderDateColumn.setTable(new Table("orders"));

        SqlColumnReference orderDateReference = resolver.resolve(orderDateColumn);
        assertNotNull(orderDateReference);
        assertEquals(new SqlTableId(null,null,"orders"), ((SqlBaseTableReference) orderDateReference.getTableReference()).getTableId());
        assertEquals("order_date", orderDateReference.getColumnName());

        // Resolve Customer.CustomerId
        Column customerIdColumn = new Column();
        customerIdColumn.setColumnName("customer_id");
        customerIdColumn.setTable(new Table("customers"));

        SqlColumnReference customerIdReference = resolver.resolve(customerIdColumn);
        assertNotNull(customerIdReference);
        assertEquals("customer_id", customerIdReference.getColumnName());
        assertEquals(new SqlTableId(null,null,"customers"), ((SqlBaseTableReference) customerIdReference.getTableReference()).getTableId());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInSubqueryWithAlias() throws Exception {
        String sql = "SELECT sub.dept_id " +
                "FROM ( " +
                "    SELECT d.id AS dept_id " +
                "    FROM departments d " +
                "    WHERE d.department_name = 'HR' " +
                ") sub";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        // Resolve alias 'dept_id' from subquery
        Column column = new Column();
        column.setColumnName("dept_id");
        column.setTable(new Table("sub"));

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("dept_id", reference.getColumnName());
        assertEquals("(SELECT d.id AS dept_id FROM departments d WHERE d.department_name = 'HR') sub",
                ((SqlDerivedTable) reference.getTableReference()).getSelect().toString());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveColumnInNestedSubqueryNoAlliases() throws Exception {
        String sql = "SELECT * FROM (SELECT first_name FROM (SELECT * FROM employees))";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference reference = resolver.resolve(column);
        assertNotNull(reference);
        assertTrue(reference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("first_name", reference.getColumnName());

        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveOuterColumn() throws Exception {
        String sql = "SELECT 1 FROM employees WHERE EXISTS (SELECT 1 FROM departments WHERE first_name = department_name)";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");

        SqlColumnReference outerReference = resolver.resolve(column);
        assertNotNull(outerReference);
        assertTrue(outerReference.getTableReference() instanceof SqlBaseTableReference);
        assertEquals("first_name", outerReference.getColumnName());
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) outerReference.getTableReference()).getTableId());

        String innerSql = "SELECT 1 FROM departments WHERE first_name = department_name";
        Select innerSelect = (Select) SqlParserUtils.parseSqlCommand(innerSql);
        resolver.enterStatementeContext(innerSelect);

        SqlColumnReference innerReference = resolver.resolve(column);
        assertNotNull(innerReference);
        assertTrue(innerReference.getTableReference() instanceof SqlBaseTableReference);
        assertEquals("first_name", innerReference.getColumnName());
        assertEquals(new SqlTableId(null,null,"employees"), ((SqlBaseTableReference) innerReference.getTableReference()).getTableId());

        resolver.exitCurrentStatementContext();
        resolver.exitCurrentStatementContext();
    }

    @Test
    void testResolveNullColumn() throws Exception {
        String sql = "SELECT e.null_value FROM (SELECT NULL AS null_value) e";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("null_value");
        column.setTable(new Table("e"));

        SqlColumnReference columnReference = resolver.resolve(column);
        assertNotNull(columnReference);
        assertEquals("null_value", columnReference.getColumnName());
        assertTrue(columnReference.getTableReference() instanceof SqlDerivedTable);
        assertEquals("(SELECT NULL AS null_value) e", ((SqlDerivedTable) columnReference.getTableReference()).getSelect().toString());
    }

    @Test
    void testResolveNonNullColumn() throws Exception {
        String sql = "SELECT non_null_value FROM (SELECT 42 AS non_null_value)";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("non_null_value");

        SqlColumnReference outerReference = resolver.resolve(column);
        assertNotNull(outerReference);
    }

    @Test
    void testResolveColumnTableNotInSchema() throws Exception {

        Assumptions.assumeTrue(this.schema.tables.stream()
                .filter(t -> t.id.name.equals("Foo"))
                .count() == 0);

        String sql = "SELECT * FROM Foo";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("bar");
        column.setTable(new Table("Foo"));

        SqlColumnReference columnReference = resolver.resolve(column);
        assertNull(columnReference);
    }

    @Test
    void testResolveTableNotInSchema() throws Exception {

        Assumptions.assumeTrue(this.schema.tables.stream()
                .filter(t -> t.id.name.equals("Foo"))
                .count() == 0);

        String sql = "SELECT * FROM Foo";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("bar");

        SqlColumnReference columnReference = resolver.resolve(column);
        assertNull(columnReference);
    }

    @Test
    void testCommonTableExpressionWithAliasForEmployees() throws JSQLParserException {
        String sql = "WITH EmployeeCTE AS (SELECT first_name, salary FROM employees WHERE salary > 50000) " +
                "SELECT e.first_name FROM EmployeeCTE e WHERE e.first_name='John' ";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column column = new Column();
        column.setColumnName("first_name");
        column.setTable(new Table("e"));

        SqlColumnReference columnReference = resolver.resolve(column);
        assertNotNull(columnReference);
        assertEquals("first_name", columnReference.getColumnName());
        assertTrue(columnReference.getTableReference() instanceof SqlDerivedTable);
        SqlDerivedTable derivedTableReference = (SqlDerivedTable) columnReference.getTableReference();
        assertEquals("(SELECT first_name, salary FROM employees WHERE salary > 50000)", derivedTableReference.getSelect().toString());
    }


    @Test
    void testResolveTableWithExplicitSchema() throws Exception {

        String sql = "SELECT user_id FROM public.users";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);


        Table publicUsers = new Table("public", "users");
        SqlTableReference tableReference = resolver.resolve(publicUsers);
        assertNotNull(tableReference);
        assertTrue(tableReference instanceof SqlBaseTableReference);
        SqlBaseTableReference baseTableReference = (SqlBaseTableReference) tableReference;

        assertEquals("users", baseTableReference.getTableId().getTableName());
        assertEquals("public", baseTableReference.getTableId().getSchemaName());
    }


    @Test
    void testResolveColumnFromExplicitSchema() throws Exception {

        String sql = "SELECT user_id FROM public.users";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);


        Column userIdColumn = new Column(null, "user_id");
        SqlColumnReference columnReference = resolver.resolve(userIdColumn);
        assertNotNull(columnReference);
        assertNotNull(columnReference.getTableReference());
        assertTrue(columnReference.getTableReference() instanceof SqlBaseTableReference);
        SqlBaseTableReference baseTableReference = (SqlBaseTableReference) columnReference.getTableReference();

        assertEquals("user_id", columnReference.getColumnName());
        assertEquals("users", baseTableReference.getTableId().getTableName());
        assertEquals("public", baseTableReference.getTableId().getSchemaName());
    }

    @Test
    void testResolveColumnFromImplicitSchema() throws Exception {

        String sql = "SELECT user_id FROM users";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Column userIdColumn = new Column(null, "user_id");
        SqlColumnReference columnReference = resolver.resolve(userIdColumn);
        assertNotNull(columnReference);
        assertNotNull(columnReference.getTableReference());
        assertTrue(columnReference.getTableReference() instanceof SqlBaseTableReference);
        SqlBaseTableReference baseTableReference = (SqlBaseTableReference) columnReference.getTableReference();

        assertEquals("user_id", columnReference.getColumnName());
        assertEquals("users", baseTableReference.getTableId().getTableName());
        assertEquals("public", baseTableReference.getTableId().getSchemaName());
    }

    @Test
    void testResolveTableAlias() throws Exception {

        String sql = "SELECT u.user_id FROM public.users u";

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        resolver.enterStatementeContext(select);

        Table tableAlias = new Table(null, "u");
        SqlTableReference tableReference = resolver.resolve(tableAlias);
        assertNotNull(tableReference);
        assertTrue(tableReference instanceof SqlBaseTableReference);
        SqlBaseTableReference baseTableReference = (SqlBaseTableReference) tableReference;

        assertEquals("users", baseTableReference.getTableId().getTableName());
        assertEquals("public", baseTableReference.getTableId().getSchemaName());
    }
}
