package org.evomaster.constraint;

public class LowerBoundConstraint extends TableConstraint {

    private final String columnName;

    private long lowerBound;

    public LowerBoundConstraint(String tableName, String columnName, long lowerBound) {
        super(tableName);
        this.columnName = columnName;
        this.lowerBound = lowerBound;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getLowerBound() {
        return lowerBound;
    }

}
