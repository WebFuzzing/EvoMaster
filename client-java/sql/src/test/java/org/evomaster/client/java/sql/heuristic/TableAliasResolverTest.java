package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableAliasResolverTest {

    @Test
    public void resolvesSimpleTableAlias() throws Exception {
        String sql = "SELECT * FROM Employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());

    }

    @Test
    public void resolvesAliasInSubquery() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM Employees) AS subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));
        SqlTableReference sqlTableReference = resolver.resolveTableReference("subquery");
        assertTrue(sqlTableReference instanceof SqlDerivedTableReference);
        assertEquals("SELECT * FROM Employees", ((SqlDerivedTableReference) sqlTableReference).getSelect().getPlainSelect().toString());
        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) ((SqlDerivedTableReference) sqlTableReference).getSelect();
        assertEquals("subquery", parenthesedSelect.getAlias().getName());
        assertEquals(1, resolver.getContextDepth());

        resolver.enterTableAliasContext(parenthesedSelect.getPlainSelect());
        assertEquals(2, resolver.getContextDepth());
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInJoin() throws Exception {
        String sql = "SELECT * FROM Employees e JOIN Departments d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInNestedSubquery() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM (SELECT * FROM Employees) AS subquery1) AS subquery2";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertEquals("SELECT * FROM (SELECT * FROM Employees) AS subquery1", ((SqlDerivedTableReference) resolver.resolveTableReference("subquery2")).getSelect().getPlainSelect().toString());

        Select subquery2 = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery2")).getSelect().getPlainSelect();
        resolver.enterTableAliasContext(subquery2);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertEquals("SELECT * FROM Employees", ((SqlDerivedTableReference) resolver.resolveTableReference("subquery1")).getSelect().getPlainSelect().toString());

        Select selectFromEmployees = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery1")).getSelect().getPlainSelect();
        resolver.enterTableAliasContext(selectFromEmployees);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));

        assertEquals(3, resolver.getContextDepth());
        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());

    }

    @Test
    public void resolvesAliasInUnion() throws Exception {
        String sql = "SELECT * FROM Employees e UNION SELECT * FROM Departments d";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));

        resolver.enterTableAliasContext(select.getSetOperationList().getSelects().get(0));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.enterTableAliasContext(select.getSetOperationList().getSelects().get(1));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInWithClause() throws Exception {
        String sql = "WITH subquery AS (SELECT * FROM Employees) SELECT * FROM subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));

        Select subquery = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery")).getSelect();
        assertEquals("(SELECT * FROM Employees)", subquery.toString());

        resolver.enterTableAliasContext(subquery);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));

        String innerSql = "SELECT * FROM subquery";
        Select innerSelect = (Select) CCJSqlParserUtil.parse(innerSql);

        resolver.exitTableAliasContext();
        resolver.enterTableAliasContext(innerSelect);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));

        resolver.exitTableAliasContext();
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());

    }

    @Test
    public void resolvesAliasInComplexJoin() throws Exception {
        String sql = "SELECT * FROM Employees e JOIN (SELECT * FROM Departments) d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        assertEquals("SELECT * FROM Departments", ((SqlDerivedTableReference) resolver.resolveTableReference("d")).getSelect().getPlainSelect().toString());

        Select subSelect = ((SqlDerivedTableReference) resolver.resolveTableReference("d")).getSelect().getPlainSelect();
        resolver.enterTableAliasContext(subSelect);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInMultipleWithClauses() throws Exception {
        String sql = "WITH subquery1 AS (SELECT * FROM Employees), subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery1, subquery2";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery2"));

        Select subquery1 = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery1")).getSelect();
        assertEquals("(SELECT * FROM Employees)", subquery1.toString());

        Select subquery2 = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery2")).getSelect();
        assertEquals("(SELECT * FROM Departments)", subquery2.toString());

        resolver.enterTableAliasContext(subquery1);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        resolver.exitTableAliasContext();

        resolver.enterTableAliasContext(subquery2);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        resolver.exitTableAliasContext();

        String query = "SELECT * FROM subquery1, subquery2";
        Select selectFrom = (Select) CCJSqlParserUtil.parse(query);
        resolver.enterTableAliasContext(selectFrom);

        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));

    }

    @Test
    public void resolvesAliasInNestedWithClauses() throws Exception {
        String sql = "WITH subquery1 AS (WITH subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery2) SELECT * FROM subquery1";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        final Select subquery1 = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery1")).getSelect();
        assertEquals("(WITH subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery2)", subquery1.toString());

        resolver.enterTableAliasContext(subquery1.getPlainSelect());
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        final Select subquery2 = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery2")).getSelect();
        assertEquals("(SELECT * FROM Departments)", subquery2.toString());

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();

        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInComplexUnion() throws Exception {
        String sql = "SELECT * FROM Employees e UNION SELECT * FROM (SELECT * FROM Departments) d";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));

        resolver.enterTableAliasContext(select.getSetOperationList().getSelects().get(0));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.enterTableAliasContext(select.getSetOperationList().getSelects().get(1));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("SELECT * FROM Departments", ((SqlDerivedTableReference) resolver.resolveTableReference("d")).getSelect().getPlainSelect().toString());

        Select subSelect = ((SqlDerivedTableReference) resolver.resolveTableReference("d")).getSelect().getPlainSelect();
        resolver.enterTableAliasContext(subSelect);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals(3, resolver.getContextDepth());

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();

        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInSubqueryWithJoin() throws Exception {
        String sql = "SELECT * FROM (SELECT e.name, d.name FROM Employees e JOIN Departments d ON e.department_id = d.department_id) AS subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));
        Select subquery = ((SqlDerivedTableReference) resolver.resolveTableReference("subquery")).getSelect().getPlainSelect();
        assertEquals("SELECT e.name, d.name FROM Employees e JOIN Departments d ON e.department_id = d.department_id", subquery.toString());

        resolver.enterTableAliasContext(subquery);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));

        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }


    @Test
    public void resolvesDuplicateAlias() throws Exception {
        String sql = "SELECT e.first_name, d.department_name " +
                "FROM ( " +
                "    SELECT id, first_name, department_id " +
                "    FROM employees e " +
                "    WHERE e.status = 'active' " +
                ") e " +
                "JOIN ( " +
                "    SELECT id, department_name " +
                "    FROM departments e " +
                "    WHERE e.is_active = 1 " +
                ") d ON e.department_id = d.id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));

        assertTrue(resolver.resolveTableReference("e") instanceof SqlDerivedTableReference);
        assertTrue(resolver.resolveTableReference("d") instanceof SqlDerivedTableReference);

        assertEquals("SELECT id, first_name, department_id FROM employees e WHERE e.status = 'active'",
                ((SqlDerivedTableReference) resolver.resolveTableReference("e")).getSelect().getPlainSelect().toString());
        assertEquals("SELECT id, department_name FROM departments e WHERE e.is_active = 1",
                ((SqlDerivedTableReference) resolver.resolveTableReference("d")).getSelect().getPlainSelect().toString());

        Select e = ((SqlDerivedTableReference) resolver.resolveTableReference("e")).getSelect().getPlainSelect();
        Select d = ((SqlDerivedTableReference) resolver.resolveTableReference("d")).getSelect().getPlainSelect();

        resolver.enterTableAliasContext(e);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertTrue(resolver.resolveTableReference("e") instanceof SqlBaseTableReference);
        assertEquals("employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.enterTableAliasContext(d);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("departments", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInDeleteStatement() throws Exception {
        String sql = "DELETE FROM Employees e WHERE e.department_id = 1";
        Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(delete);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInUpdateStatement() throws Exception {
        String sql = "UPDATE Employees e SET e.salary = e.salary * 1.1 WHERE e.department_id = 1";
        Update update = (Update) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(update);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInDeleteWithJoin() throws Exception {
        String sql = "DELETE e FROM Employees e JOIN Departments d ON e.department_id = d.id WHERE d.name = 'HR'";
        Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(delete);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInUpdateWithJoin() throws Exception {
        String sql = "UPDATE Employees e JOIN Departments d ON e.department_id = d.id SET e.salary = e.salary * 1.1 WHERE d.name = 'Engineering'";
        Update update = (Update) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(update);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInDeleteWithWithClause() throws Exception {
        String sql = "WITH dept_to_delete AS (SELECT id FROM Departments WHERE name = 'HR') " +
                "DELETE FROM Employees e WHERE e.department_id IN (SELECT id FROM dept_to_delete)";
        Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(delete);

        // Verify the alias in WITH clause
        assertTrue(resolver.isAliasDeclaredInCurrentContext("dept_to_delete"));
        SqlTableReference withTableReference = resolver.resolveTableReference("dept_to_delete");
        assertTrue(withTableReference instanceof SqlDerivedTableReference);
        assertEquals("SELECT id FROM Departments WHERE name = 'HR'",
                ((SqlDerivedTableReference) withTableReference).getSelect().getPlainSelect().toString());

        // Verify the alias in the DELETE statement
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInMultiTableDelete() throws Exception {
        String sql = "DELETE e, d FROM Employees e JOIN Departments d ON e.department_id = d.id WHERE d.name = 'HR'";
        Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(delete);

        // Verify the aliases for both tables
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInSelectWithInSubquery() throws Exception {
        String sql = "SELECT e.first_name FROM Employees e WHERE e.department_id IN (SELECT d.id FROM Departments d WHERE d.name = 'HR')";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);

        // Verify alias in the main query
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        // Access the subquery in IN expression
        InExpression inExpression = (InExpression) ((PlainSelect) select).getWhere();
        Select subquery = (Select) inExpression.getRightExpression();
        resolver.enterTableAliasContext(subquery);

        // Verify alias in the subquery
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Departments", ((SqlBaseTableReference) resolver.resolveTableReference("d")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasCaseInsensitivity() throws Exception {
        String sql = "SELECT * FROM Employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver();
        resolver.enterTableAliasContext(select);

        // Check case-sensitive alias resolution
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("E")); // Case-sensitive check
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasCaseSensitivity() throws Exception {
        String sql = "SELECT * FROM Employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        boolean isCaseSensitive = true;
        TableAliasResolver resolver = new TableAliasResolver(isCaseSensitive);
        resolver.enterTableAliasContext(select);

        // Check case-sensitive alias resolution
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("E")); // Case-sensitive check
        assertEquals("Employees", ((SqlBaseTableReference) resolver.resolveTableReference("e")).getFullyQualifiedName());

        resolver.exitTableAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }
}
