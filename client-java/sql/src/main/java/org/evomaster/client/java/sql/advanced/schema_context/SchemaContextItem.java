package org.evomaster.client.java.sql.advanced.schema_context;

import org.evomaster.client.java.sql.advanced.driver.Schema;
import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;

import java.util.List;
import java.util.stream.Collectors;

public class SchemaContextItem {

    private List<TableColumnsNames> tablesColumnsNames;

    private SchemaContextItem(List<TableColumnsNames> tablesColumnsNames) {
        this.tablesColumnsNames = tablesColumnsNames;
    }

    public static SchemaContextItem createSchemaContextItem(List<QueryTable> tables, Schema schema) {
        List<TableColumnsNames> tableColumnsNames = tables.stream()
            .map(table -> new TableColumnsNames(table, schema.getTables().get(table.getName())))
            .collect(Collectors.toList());
        return new SchemaContextItem(tableColumnsNames);
    }

    public Boolean includes(QueryColumn column) {
        return tablesColumnsNames.stream().anyMatch(tableColumnsNames -> tableColumnsNames.includes(column));
    }

    @Override
    public String toString() {
        return tablesColumnsNames.toString();
    }
}
