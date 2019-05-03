package org.evomaster.client.java.controller.internal.db.constraint;

public class UpperBoundConstraint extends TableConstraint {

    private final String columnName;

    private final long upperBound;

    public UpperBoundConstraint(String tableName, String columnName, long upperBound) {
        super(tableName);
        this.columnName = columnName;
        this.upperBound = upperBound;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getUpperBound() {
        return this.upperBound;
    }

}
