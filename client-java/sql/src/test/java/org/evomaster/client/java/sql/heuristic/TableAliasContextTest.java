package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TableAliasContextTest {


    @Test
    public void testContainsAliasWithNull() {
        TableAliasContext context = new TableAliasContext();
        assertThrows(NullPointerException.class, () -> context.containsAlias((String) null));
    }

    @Test
    public void testAddAliasToTableNameWithNullAlias() {
        Table table = new Table("Employees");
        TableAliasContext context = new TableAliasContext();
        assertThrows(NullPointerException.class, () -> context.addAliasToTableName(null, table));
    }

    @Test
    public void testAddAliasToTableNameWithNullTable() {
        Alias alias = new Alias("e");
        TableAliasContext context = new TableAliasContext();
        assertThrows(NullPointerException.class, () -> context.addAliasToTableName(alias, null));
    }

    @Test
    public void testAddAliasToTableName() {
        Alias alias = new Alias("e");
        Table table = new Table("Employees");

        TableAliasContext context = new TableAliasContext();
        context.addAliasToTableName(alias, table);

        assertTrue(context.containsAlias("e"));
        assertTrue(context.containsAlias("E"));

        SqlTableReference ref = context.getTableReference("e");
        assertTrue(ref instanceof SqlTableName);
        assertEquals("Employees", ((SqlTableName) ref).getTable().getName());
    }

    @Test
    public void testAddDuplicateAliasToTableName() {
        Alias alias1 = new Alias("e");
        Table table1 = new Table("Employees");

        TableAliasContext context = new TableAliasContext();
        context.addAliasToTableName(alias1, table1);

        Alias alias2 = new Alias("E"); // Case insensitive check
        Table table2 = new Table("Engineers");

        assertThrows(IllegalArgumentException.class, () -> context.addAliasToTableName(alias2, table2));
    }

    @Test
    public void testAddAliasToDerivedTable() throws Exception {
        Alias alias = new Alias("sub");
        Select subquery = (Select) CCJSqlParserUtil.parse("SELECT * FROM Employees");

        TableAliasContext context = new TableAliasContext();
        context.addAliasToDerivedTable(alias, subquery);

        assertTrue(context.containsAlias("sub"));
        assertTrue(context.containsAlias("SUB"));

        SqlTableReference ref = context.getTableReference("sub");
        assertTrue(ref instanceof SqlDerivedTable);
        assertEquals(subquery, ((SqlDerivedTable) ref).getSelect());
    }

    @Test
    public void testAddDuplicateAliasToDerivedTable() throws Exception {
        Alias alias1 = new Alias("sub");
        Select subquery1 = (Select) CCJSqlParserUtil.parse("SELECT * FROM Employees");

        TableAliasContext context = new TableAliasContext();
        context.addAliasToDerivedTable(alias1, subquery1);

        Alias alias2 = new Alias("sub");
        Select subquery2 = (Select) CCJSqlParserUtil.parse("SELECT * FROM Departments");

        assertThrows(IllegalArgumentException.class, () -> context.addAliasToDerivedTable(alias2, subquery2));
    }

    @Test
    public void testAddMixedDuplicateAliases() throws Exception {
        Alias alias1 = new Alias("a");
        Table table = new Table("T");

        TableAliasContext context = new TableAliasContext();
        context.addAliasToTableName(alias1, table);

        Alias alias2 = new Alias("a");
        Select subquery = (Select) CCJSqlParserUtil.parse("SELECT * FROM T2");

        assertThrows(IllegalArgumentException.class, () -> context.addAliasToDerivedTable(alias2, subquery));
    }

    @Test
    public void testGetTableReferenceNonExisting() {
        TableAliasContext context = new TableAliasContext();
        assertThrows(IllegalArgumentException.class, () -> context.getTableReference("nonexistent"));
    }

    @Test
    public void testGetTableReferenceCaseInsensitivity() {
        Alias alias = new Alias("MyAlias");
        Table table = new Table("MyTable");

        TableAliasContext context = new TableAliasContext();
        context.addAliasToTableName(alias, table);

        assertNotNull(context.getTableReference("myalias"));
        assertNotNull(context.getTableReference("MYALIAS"));
        assertNotNull(context.getTableReference("MyAlias"));
    }
}
