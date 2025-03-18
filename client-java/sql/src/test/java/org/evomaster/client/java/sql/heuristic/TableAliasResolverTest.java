package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TableAliasResolverTest {

    @Test
    public void resolvesSimpleTableAlias() throws Exception {
        String sql = "SELECT * FROM Employees e";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(1, aliasMap.size());
        assertTrue(aliasMap.containsKey("e"));
        assertEquals("Employees", aliasMap.get("e").getBaseTableName());
    }

    @Test
    public void resolvesAliasInSubquery() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM Employees) AS subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(1, aliasMap.size());
        assertTrue(aliasMap.containsKey("subquery"));
        TableReference tableReference = aliasMap.get("subquery");
        assertEquals(true, tableReference.isDerivedTableReference());
        assertEquals("SELECT * FROM Employees", tableReference.getDerivedTableSelect().getPlainSelect().toString());
        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) tableReference.getDerivedTableSelect();
        assertEquals("subquery" , parenthesedSelect.getAlias().getName());

    }

    @Test
    public void resolvesAliasInJoin() throws Exception {
        String sql = "SELECT * FROM Employees e JOIN Departments d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(2, aliasMap.size());
        assertTrue(aliasMap.containsKey("e"));
        assertTrue(aliasMap.containsKey("d"));
        assertEquals("Employees", aliasMap.get("e").getBaseTableName());
        assertEquals("Departments", aliasMap.get("d").getBaseTableName());
    }

    @Test
    public void resolvesAliasInNestedSubquery() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM (SELECT * FROM Employees) AS subquery1) AS subquery2";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertTrue(aliasMap.containsKey("subquery1"));
        assertTrue(aliasMap.containsKey("subquery2"));
        assertEquals(2, aliasMap.size());
        assertEquals("SELECT * FROM Employees", aliasMap.get("subquery1").getDerivedTableSelect().getPlainSelect().toString());
        assertEquals("SELECT * FROM (SELECT * FROM Employees) AS subquery1", aliasMap.get("subquery2").getDerivedTableSelect().getPlainSelect().toString());
    }

    @Test
    public void resolvesAliasInUnion() throws Exception {
        String sql = "SELECT * FROM Employees e UNION SELECT * FROM Departments d";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(2, aliasMap.size());
        assertTrue(aliasMap.containsKey("e"));
        assertTrue(aliasMap.containsKey("d"));
        assertEquals("Employees", aliasMap.get("e").getBaseTableName());
        assertEquals("Departments", aliasMap.get("d").getBaseTableName());
    }

    @Test
    public void resolvesAliasInWithClause() throws Exception {
        String sql = "WITH subquery AS (SELECT * FROM Employees) SELECT * FROM subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(1, aliasMap.size());
        assertTrue(aliasMap.containsKey("subquery"));
        WithItem withItem = (WithItem) aliasMap.get("subquery").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Employees)", withItem.getSelect().toString());
    }

    @Test
    public void resolvesAliasInComplexJoin() throws Exception {
        String sql = "SELECT * FROM Employees e JOIN (SELECT * FROM Departments) d ON e.department_id = d.department_id";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(2, aliasMap.size());
        assertTrue(aliasMap.containsKey("e"));
        assertTrue(aliasMap.containsKey("d"));
        assertEquals("Employees", aliasMap.get("e").getBaseTableName());
        assertEquals("SELECT * FROM Departments", aliasMap.get("d").getDerivedTableSelect().getPlainSelect().toString());
    }

    @Test
    public void resolvesAliasInMultipleWithClauses() throws Exception {
        String sql = "WITH subquery1 AS (SELECT * FROM Employees), subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery1, subquery2";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(2, aliasMap.size());
        assertTrue(aliasMap.containsKey("subquery1"));
        assertTrue(aliasMap.containsKey("subquery2"));
        WithItem subqueryWithItem1 = (WithItem) aliasMap.get("subquery1").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Employees)", subqueryWithItem1.getSelect().toString());
        WithItem subqueryWithItem2 = (WithItem) aliasMap.get("subquery2").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Departments)", subqueryWithItem2.getSelect().toString());
    }

    @Test
    public void resolvesAliasInNestedWithClauses() throws Exception {
        String sql = "WITH subquery1 AS (WITH subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery2) SELECT * FROM subquery1";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertTrue(aliasMap.containsKey("subquery1"));
        final WithItem subquery1 = (WithItem) aliasMap.get("subquery1").getDerivedTableSelect();
        assertEquals("(WITH subquery2 AS (SELECT * FROM Departments) SELECT * FROM subquery2)", subquery1.getSelect().toString());

        assertTrue(aliasMap.containsKey("subquery2"));
        final WithItem subquery2 = (WithItem) aliasMap.get("subquery2").getDerivedTableSelect();
        assertEquals("(SELECT * FROM Departments)", subquery2.getSelect().toString());

        assertEquals(2, aliasMap.size());

    }

    @Test
    public void resolvesAliasInComplexUnion() throws Exception {
        String sql = "SELECT * FROM Employees e UNION SELECT * FROM (SELECT * FROM Departments) d";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(2, aliasMap.size());
        assertTrue(aliasMap.containsKey("e"));
        assertTrue(aliasMap.containsKey("d"));
        assertEquals("Employees", aliasMap.get("e").getBaseTableName());
        assertEquals("SELECT * FROM Departments", aliasMap.get("d").getDerivedTableSelect().getPlainSelect().toString());
    }

    @Test
    public void resolvesAliasInSubqueryWithJoin() throws Exception {
        String sql = "SELECT * FROM (SELECT e.name, d.name FROM Employees e JOIN Departments d ON e.department_id = d.department_id) AS subquery";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        TableAliasResolver resolver = new TableAliasResolver(select);
        Map<String, TableReference> aliasMap = resolver.getAliasMap();
        assertEquals(3, aliasMap.size());
        assertTrue(aliasMap.containsKey("subquery"));
        assertTrue(aliasMap.containsKey("d"));
        assertTrue(aliasMap.containsKey("e"));
        final Select subquery = aliasMap.get("subquery").getDerivedTableSelect();
        final TableReference e = aliasMap.get("e");
        final TableReference d = aliasMap.get("d");
        assertEquals("SELECT e.name, d.name FROM Employees e JOIN Departments d ON e.department_id = d.department_id", subquery.getPlainSelect().toString());
        assertEquals("Employees", e.getBaseTableName());
        assertEquals("Departments", d.getBaseTableName());
    }

}
