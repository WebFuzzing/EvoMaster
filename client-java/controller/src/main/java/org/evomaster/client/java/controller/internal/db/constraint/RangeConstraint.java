package org.evomaster.client.java.controller.internal.db.constraint;

public class RangeConstraint extends SchemaConstraint {

    private final String tableName;

    private final String columnName;

    private final long minValue;

    private final long maxValue;

    public RangeConstraint(String tableName, String columnName, long minValue, long maxValue) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }


    public String getTableName() {
        return this.tableName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getMinValue() {
        return this.minValue;
    }

    public long getMaxValue() {
        return this.maxValue;
    }

}
