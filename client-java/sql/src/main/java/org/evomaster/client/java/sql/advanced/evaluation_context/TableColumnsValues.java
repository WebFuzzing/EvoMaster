package org.evomaster.client.java.sql.advanced.evaluation_context;

import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;

import java.util.Map;

import static java.lang.String.format;

public class TableColumnsValues extends TableColumnsContainer {

    private Map<String, Object> columnValues;

    public TableColumnsValues(QueryTable table, Map<String, Object> columnValues) {
        super(table);
        this.columnValues = columnValues;
    }

    public Object getValue(QueryColumn column) {
        return columnValues.get(column.getName());
    }

    @Override
    protected Boolean columnIsInThisTable(String columnName) {
        return columnValues.containsKey(columnName);
    }

    @Override
    public String toString() {
        return format("%s: %s", getTable(), columnValues);
    }
}
