package org.evomaster.dbconstraint;

import java.util.Objects;

public class RangeConstraint extends TableConstraint {

    private final /*non-null*/ String columnName;

    private final long minValue;

    private final long maxValue;

    public RangeConstraint(String tableName, String columnName, long minValue, long maxValue) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
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

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
