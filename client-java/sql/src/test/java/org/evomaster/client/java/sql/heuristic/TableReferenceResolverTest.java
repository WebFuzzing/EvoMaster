package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableReferenceResolverTest {

    @Test
    public void resolvesSimpleTableAlias() throws Exception {
        String sql = "SELECT * FROM Employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue( resolver.isAliasDeclaredInCurrentContext("e"));
        assertEquals("Employees",  resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());

    }

    @Test
    public void resolvesAliasInSubquery() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM Employees) AS subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));
        TableReference tableReference = resolver.resolveTableReference("subquery");
        assertEquals(true, tableReference.isDerivedTableReference());
        assertEquals("SELECT * FROM Employees", tableReference.getDerivedTableSelect().getPlainSelect().toString());
        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) tableReference.getDerivedTableSelect();
        assertEquals("subquery" , parenthesedSelect.getAlias().getName());
        assertEquals(1, resolver.getContextDepth());

        resolver.enterAliasContext(parenthesedSelect.getPlainSelect());
        assertEquals(2, resolver.getContextDepth());
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));

        resolver.exitAliasContext();
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInJoin() throws Exception {
        String sql = "SELECT * FROM Employees e JOIN Departments d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());
        assertEquals("Departments", resolver.resolveTableReference("d").getBaseTable().getFullyQualifiedName());
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInNestedSubquery() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM (SELECT * FROM Employees) AS subquery1) AS subquery2";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertEquals("SELECT * FROM (SELECT * FROM Employees) AS subquery1", resolver.resolveTableReference("subquery2").getDerivedTableSelect().getPlainSelect().toString());

        Select subquery2 = resolver.resolveTableReference("subquery2").getDerivedTableSelect().getPlainSelect();
        resolver.enterAliasContext(subquery2);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertEquals("SELECT * FROM Employees", resolver.resolveTableReference("subquery1").getDerivedTableSelect().getPlainSelect().toString());

        Select selectFromEmployees = resolver.resolveTableReference("subquery1").getDerivedTableSelect().getPlainSelect();
        resolver.enterAliasContext(selectFromEmployees);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));

        assertEquals(3, resolver.getContextDepth());
        resolver.exitAliasContext();
        resolver.exitAliasContext();
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());

    }

    @Test
    public void resolvesAliasInUnion() throws Exception {
        String sql = "SELECT * FROM Employees e UNION SELECT * FROM Departments d";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));

        resolver.enterAliasContext(select.getSetOperationList().getSelects().get(0));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());

        resolver.exitAliasContext();
        resolver.enterAliasContext(select.getSetOperationList().getSelects().get(1));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Departments", resolver.resolveTableReference("d").getBaseTable().getFullyQualifiedName());

        resolver.exitAliasContext();
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInWithClause() throws Exception {
        String sql = "WITH subquery AS (SELECT * FROM Employees) SELECT * FROM subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));

        Select subquery =  resolver.resolveTableReference("subquery").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Employees)", subquery.toString());

        resolver.enterAliasContext(subquery);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));

        String innerSql = "SELECT * FROM subquery";
        Select innerSelect = (Select) CCJSqlParserUtil.parse(innerSql);

        resolver.exitAliasContext();
        resolver.enterAliasContext(innerSelect);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));

        resolver.exitAliasContext();
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));

        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());

    }

    @Test
    public void resolvesAliasInComplexJoin() throws Exception {
        String sql = "SELECT * FROM Employees e JOIN (SELECT * FROM Departments) d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());
        assertEquals("SELECT * FROM Departments", resolver.resolveTableReference("d").getDerivedTableSelect().getPlainSelect().toString());

        Select subSelect = resolver.resolveTableReference("d").getDerivedTableSelect().getPlainSelect();
        resolver.enterAliasContext(subSelect);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));

        resolver.exitAliasContext();
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInMultipleWithClauses() throws Exception {
        String sql = "WITH subquery1 AS (SELECT * FROM Employees), subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery1, subquery2";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery2"));

        Select subquery1 = resolver.resolveTableReference("subquery1").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Employees)", subquery1.toString());

        Select subquery2 = resolver.resolveTableReference("subquery2").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Departments)", subquery2.toString());

        resolver.enterAliasContext(subquery1);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        resolver.exitAliasContext();

        resolver.enterAliasContext(subquery2);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        resolver.exitAliasContext();

        String query = "SELECT * FROM subquery1, subquery2";
        Select selectFrom = (Select) CCJSqlParserUtil.parse(query);
        resolver.enterAliasContext(selectFrom);

        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery2"));

    }

    @Test
    public void resolvesAliasInNestedWithClauses() throws Exception {
        String sql = "WITH subquery1 AS (WITH subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery2) SELECT * FROM subquery1";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery1"));
        final Select subquery1 = resolver.resolveTableReference("subquery1").getDerivedTableSelect();
        assertEquals("(WITH subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery2)", subquery1.toString());

        resolver.enterAliasContext(subquery1.getPlainSelect());
        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery2"));
        final Select subquery2 = resolver.resolveTableReference("subquery2").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Departments)", subquery2.toString());

        resolver.exitAliasContext();
        resolver.exitAliasContext();

        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInComplexUnion() throws Exception {
        String sql = "SELECT * FROM Employees e UNION SELECT * FROM (SELECT * FROM Departments) d";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));

        resolver.enterAliasContext(select.getSetOperationList().getSelects().get(0));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("Employees", resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());

        resolver.exitAliasContext();
        resolver.enterAliasContext(select.getSetOperationList().getSelects().get(1));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("SELECT * FROM Departments", resolver.resolveTableReference("d").getDerivedTableSelect().getPlainSelect().toString());

        Select subSelect = resolver.resolveTableReference("d").getDerivedTableSelect().getPlainSelect();
        resolver.enterAliasContext(subSelect);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals(3, resolver.getContextDepth());

        resolver.exitAliasContext();
        resolver.exitAliasContext();
        resolver.exitAliasContext();

        assertEquals(0, resolver.getContextDepth());
    }

    @Test
    public void resolvesAliasInSubqueryWithJoin() throws Exception {
        String sql = "SELECT * FROM (SELECT e.name, d.name FROM Employees e JOIN Departments d ON e.department_id = d.department_id) AS subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("subquery"));
        Select subquery = resolver.resolveTableReference("subquery").getDerivedTableSelect().getPlainSelect();
        assertEquals("SELECT e.name, d.name FROM Employees e JOIN Departments d ON e.department_id = d.department_id", subquery.toString());

        resolver.enterAliasContext(subquery);
        assertFalse(resolver.isAliasDeclaredInCurrentContext("subquery"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));

        assertEquals("Employees", resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());
        assertEquals("Departments", resolver.resolveTableReference("d").getBaseTable().getFullyQualifiedName());

        resolver.exitAliasContext();
        resolver.exitAliasContext();
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
        TableReferenceResolver resolver = new TableReferenceResolver();
        resolver.enterAliasContext(select);

        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertTrue(resolver.isAliasDeclaredInCurrentContext("d"));

        assertTrue(resolver.resolveTableReference("e").isDerivedTableReference());
        assertTrue(resolver.resolveTableReference("d").isDerivedTableReference());

        assertEquals("SELECT id, first_name, department_id FROM employees e WHERE e.status = 'active'",
                resolver.resolveTableReference("e").getDerivedTableSelect().getPlainSelect().toString());
        assertEquals("SELECT id, department_name FROM departments e WHERE e.is_active = 1",
                resolver.resolveTableReference("d").getDerivedTableSelect().getPlainSelect().toString());

        Select e= resolver.resolveTableReference("e").getDerivedTableSelect().getPlainSelect();
        Select d= resolver.resolveTableReference("d").getDerivedTableSelect().getPlainSelect();

        resolver.enterAliasContext(e);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertTrue(resolver.resolveTableReference("e").isBaseTableReference());
        assertEquals("employees" , resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());

        resolver.exitAliasContext();
        resolver.enterAliasContext(d);
        assertTrue(resolver.isAliasDeclaredInCurrentContext("e"));
        assertFalse(resolver.isAliasDeclaredInCurrentContext("d"));
        assertEquals("departments" , resolver.resolveTableReference("e").getBaseTable().getFullyQualifiedName());

        resolver.exitAliasContext();
        resolver.exitAliasContext();
        assertEquals(0, resolver.getContextDepth());
    }

}
