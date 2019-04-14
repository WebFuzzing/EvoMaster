package org.evomaster.client.java.controller.internal.db.constraint;

public class LowerBoundConstraint extends SchemaConstraint {

    private final String tableName;

    private final String columnName;

    private long lowerBound;

    public LowerBoundConstraint(String tableName, String columnName, long lowerBound) {
        super();
        this.tableName = tableName;
        this.columnName = columnName;
        this.lowerBound = lowerBound;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getLowerBound() {
        return lowerBound;
    }

    public String getTableName() {
        return this.tableName;
    }
}
