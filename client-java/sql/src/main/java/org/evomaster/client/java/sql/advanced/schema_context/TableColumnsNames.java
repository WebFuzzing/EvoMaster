package org.evomaster.client.java.sql.advanced.schema_context;

import org.evomaster.client.java.sql.advanced.evaluation_context.TableColumnsContainer;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;

import java.util.List;

import static java.lang.String.format;

public class TableColumnsNames extends TableColumnsContainer {

    private List<String> columnNames;

    public TableColumnsNames(QueryTable table, List<String> columnNames) {
        super(table);
        this.columnNames = columnNames;
    }

    @Override
    protected Boolean columnIsInThisTable(String columnName) {
        return columnNames.contains(columnName);
    }

    @Override
    public String toString() {
        return format("%s: %s", getTable(), columnNames);
    }
}
