package org.evomaster.client.java.sql.advanced.evaluation_context;

import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;

public abstract class TableColumnsContainer {

    private QueryTable table;

    protected TableColumnsContainer(QueryTable table) {
        this.table = table;
    }

    protected QueryTable getTable() {
        return table;
    }

    protected Boolean includes(QueryColumn column, Boolean exactMatch) {
        return (column.hasTable() && isThisTable(column.getTableName())) ||
            ((!column.hasTable() || !exactMatch) && columnIsInThisTable(column.getName()));
    }

    private Boolean isThisTable(String columnTableName) {
        return hasSameNameThanThisTable(columnTableName) || hasSameAliasThanThisTable(columnTableName);
    }

    private Boolean hasSameNameThanThisTable(String columnTableName) {
        return columnTableName.equals(table.getName());
    }

    private Boolean hasSameAliasThanThisTable(String columnTableName) {
        return table.hasAlias() && columnTableName.equals(table.getAlias());
    }

    protected abstract Boolean columnIsInThisTable(String columnName);
}
