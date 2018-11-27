package org.evomaster.client.java.controller.internal.db;

public class UpperBoundConstraint extends SchemaConstraint {

    private final String tableName;

    private final String columnName;

    private final long upperBound;

    public UpperBoundConstraint(String tableName, String columnName, long upperBound) {
        super();
        this.tableName = tableName;
        this.columnName = columnName;
        this.upperBound = upperBound;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getUpperBound() {
        return this.upperBound;
    }

}
