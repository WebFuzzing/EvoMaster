package org.evomaster.constraint;

public class RangeConstraint extends TableConstraint {

    private final String columnName;

    private final long minValue;

    private final long maxValue;

    public RangeConstraint(String tableName, String columnName, long minValue, long maxValue) {
        super(tableName);
        this.columnName = columnName;
        this.minValue = minValue;
        this.maxValue = maxValue;
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
