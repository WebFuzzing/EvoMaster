package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TablesNamesFinderTest {

    @Test
    void findsSingleTableInSimpleQuery() throws JSQLParserException {
        String sql = "SELECT * FROM Users";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(1, tables.size());
        assertTrue(tables.contains("Users"));
    }

    @Test
    void findsMultipleTablesInJoinQuery() throws JSQLParserException {
        String sql = "SELECT e.name, d.department_name FROM Employees e JOIN Departments d ON e.department_id = d.id";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(2, tables.size());
        assertTrue(tables.contains("Employees"));
        assertTrue(tables.contains("Departments"));
    }

    @Test
    void findsTableInSubquery() throws JSQLParserException {
        String sql = "SELECT name FROM (SELECT name, age FROM Users WHERE age > 30) AS subquery";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(1, tables.size());
        assertTrue(tables.contains("Users"));
    }

    @Test
    void handlesQueryWithoutTables() throws JSQLParserException {
        String sql = "SELECT 1";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertTrue(tables.isEmpty());
    }

    @Test
    void findsAliasedTableNames() throws JSQLParserException {
        String sql = "SELECT u.name FROM Users u";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(1, tables.size());
        assertTrue(tables.contains("Users"));
    }

    @Test
    void handlesCaseInsensitiveKeywords() throws JSQLParserException {
        String sql = "select * from USERS";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(1, tables.size());
        assertTrue(tables.contains("USERS"));
    }

    @Test
    void findsTablesInComplexQuery() throws JSQLParserException {
        String sql = "SELECT e.name, d.name FROM Employees e, Departments d WHERE e.department_id = d.id";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(2, tables.size());
        assertTrue(tables.contains("Employees"));
        assertTrue(tables.contains("Departments"));
    }

    @Test
    void handlesQueryWithUnion() throws JSQLParserException {
        String sql = "SELECT name FROM Users UNION SELECT name FROM Admins";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(2, tables.size());
        assertTrue(tables.contains("Users"));
        assertTrue(tables.contains("Admins"));
    }

    @Test
    void handlesQueryWithNestedSubqueries() throws JSQLParserException {
        String sql = "SELECT * FROM (SELECT * FROM (SELECT * FROM Users) AS innerSubquery) AS outerSubquery";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);
        assertEquals(1, tables.size());
        assertTrue(tables.contains("Users"));
    }

    @Test
    void findsTablesInUnionWithSubqueries() throws JSQLParserException {
        String sql = "SELECT name FROM (SELECT name FROM Users WHERE age > 30) AS subquery1 " +
                "UNION " +
                "SELECT name FROM (SELECT name FROM Admins WHERE role = 'manager') AS subquery2";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(2, tables.size());
        assertTrue(tables.contains("Users"));
        assertTrue(tables.contains("Admins"));
    }

    @Test
    void findsTablesInNestedUnionsAndSubqueries() throws JSQLParserException {
        String sql = "SELECT * FROM (" +
                "  SELECT * FROM (" +
                "    SELECT * FROM Users " +
                "    UNION " +
                "    SELECT * FROM Admins" +
                "  ) AS innerUnion " +
                "  UNION " +
                "  SELECT * FROM Employees" +
                ") AS outerUnion";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(3, tables.size());
        assertTrue(tables.contains("Users"));
        assertTrue(tables.contains("Admins"));
        assertTrue(tables.contains("Employees"));
    }

    @Test
    void findsTablesInUnionWithMultipleSubqueries() throws JSQLParserException {
        String sql = "SELECT name FROM (" +
                "  SELECT name FROM Users WHERE age > 30 " +
                "  UNION " +
                "  SELECT name FROM Admins WHERE role = 'manager'" +
                ") AS subquery1 " +
                "UNION " +
                "SELECT name FROM Employees";
        Statement statement = CCJSqlParserUtil.parse(sql);

        TablesNamesFinder finder = new TablesNamesFinder();
        Set<String> tables = finder.getTables(statement);

        assertEquals(3, tables.size());
        assertTrue(tables.contains("Users"));
        assertTrue(tables.contains("Admins"));
        assertTrue(tables.contains("Employees"));
    }

}
