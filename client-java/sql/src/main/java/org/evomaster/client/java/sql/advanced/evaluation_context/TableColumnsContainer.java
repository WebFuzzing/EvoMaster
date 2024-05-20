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

    public Boolean includes(QueryColumn column) {
        return (column.hasTable() && isThisTable(column.getTable())) ||
            (!column.hasTable() && columnIsInThisTable(column.getName()));
    }

    private Boolean isThisTable(QueryTable columnTable) {
        return hasSameName(table, columnTable) || hasSameAlias(table, columnTable);
    }

    private Boolean hasSameName(QueryTable thisTable, QueryTable columnTable) {
        return columnTable.getName().equals(thisTable.getName());
    }

    private Boolean hasSameAlias(QueryTable thisTable, QueryTable columnTable) {
        return thisTable.hasAlias() && columnTable.getName().equals(thisTable.getAlias());
    }

    protected abstract Boolean columnIsInThisTable(String columnName);
}
