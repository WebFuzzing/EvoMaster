package org.evomaster.client.java.sql.heuristic;

public class ColumnReference {

    private final TableReference tableReference;
    private final String columnName;

    public ColumnReference(TableReference tableReference, String columnName) {
        this.tableReference = tableReference;
        this.columnName = columnName;
    }

    public TableReference getTableReference() {
        return tableReference;
    }

    public String getColumnName() {
        return columnName;
    }
}
