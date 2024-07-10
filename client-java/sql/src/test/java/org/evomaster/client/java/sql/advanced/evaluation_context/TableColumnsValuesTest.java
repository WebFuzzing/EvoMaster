package org.evomaster.client.java.sql.advanced.evaluation_context;

import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;
import org.junit.Test;

import java.util.Map;

import static org.evomaster.client.java.sql.advanced.CollectionsHelper.createMap;
import static org.evomaster.client.java.sql.advanced.select_query.QueryColumn.createQueryColumn;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;
import static org.junit.Assert.*;

public class TableColumnsValuesTest {

    @Test
    public void testIncludes() { //Exact match using column without name
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("column");
        assertTrue(tableColumnsValues.includes(column, true));
    }

    @Test
    public void testIncludes2() { //Exact match using column with table name
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("table", "column");
        assertTrue(tableColumnsValues.includes(column, true));
    }

    @Test
    public void testIncludes3() { //Exact match using column with table alias
        QueryTable table = createQueryTable("table", "alias");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("alias", "column");
        assertTrue(tableColumnsValues.includes(column, true));
    }

    @Test
    public void testIncludes4() { //Loose match
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("alias", "column");
        assertTrue(tableColumnsValues.includes(column, false));
    }

    @Test
    public void testNotIncludes() { //Different column name
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("other_column");
        assertFalse(tableColumnsValues.includes(column, true));
    }
    
    @Test
    public void testNotIncludes2() { //Exact match using column with different table name
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("other_table", "column");
        assertFalse(tableColumnsValues.includes(column, true));
    }

    @Test
    public void testNotIncludes3() { //Exact match using column with different table alias
        QueryTable table = createQueryTable("table", "alias");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("other_alias", "column");
        assertFalse(tableColumnsValues.includes(column, true));
    }

    @Test
    public void testGetValue() {
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("column");
        assertEquals("value", tableColumnsValues.getValue(column));
    }
}
