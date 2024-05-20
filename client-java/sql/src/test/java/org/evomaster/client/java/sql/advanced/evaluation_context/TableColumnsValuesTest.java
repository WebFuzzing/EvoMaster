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
    public void testIncludes() {
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("column");
        assertTrue(tableColumnsValues.includes(column));
    }

    @Test
    public void testIncludes2() {
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn(createQueryTable("table"), "column");
        assertTrue(tableColumnsValues.includes(column));
    }

    @Test
    public void testIncludes3() {
        QueryTable table = createQueryTable("table", "alias");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn(createQueryTable("alias"), "column");
        assertTrue(tableColumnsValues.includes(column));
    }

    @Test
    public void testNotIncludes() {
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn("other_column");
        assertFalse(tableColumnsValues.includes(column));
    }

    @Test
    public void testNotIncludes2() {
        QueryTable table = createQueryTable("table");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn(createQueryTable("other_table"), "column");
        assertFalse(tableColumnsValues.includes(column));
    }

    @Test
    public void testNotIncludes3() {
        QueryTable table = createQueryTable("table", "alias");
        Map<String, Object> columnValues = createMap("column", "value");
        TableColumnsValues tableColumnsValues = new TableColumnsValues(table, columnValues);
        QueryColumn column = createQueryColumn(createQueryTable("other_alias"), "column");
        assertFalse(tableColumnsValues.includes(column));
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
