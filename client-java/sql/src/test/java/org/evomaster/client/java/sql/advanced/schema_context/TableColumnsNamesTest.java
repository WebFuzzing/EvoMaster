package org.evomaster.client.java.sql.advanced.schema_context;

import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.evomaster.client.java.sql.advanced.select_query.QueryColumn.createQueryColumn;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableColumnsNamesTest {

    @Test
    public void testIncludes() {
        QueryTable table = createQueryTable("table");
        List<String> columnNames = singletonList("column");
        TableColumnsNames tableColumnsNames = new TableColumnsNames(table, columnNames);
        QueryColumn column = createQueryColumn("column");
        assertTrue(tableColumnsNames.includes(column));
    }

    @Test
    public void testNotIncludes() {
        QueryTable table = createQueryTable("table");
        List<String> columnNames = singletonList("column");
        TableColumnsNames tableColumnsNames = new TableColumnsNames(table, columnNames);
        QueryColumn column = createQueryColumn("other_column");
        assertFalse(tableColumnsNames.includes(column));
    }
}
