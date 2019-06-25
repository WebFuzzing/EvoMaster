package org.evomaster.dbconstraint;

import java.util.Objects;

/**
 * Represents the constraint table.column <= value
 */
public class UpperBoundConstraint extends TableConstraint {

    private final /*non-null*/ String columnName;

    private final long upperBound;

    public UpperBoundConstraint(String tableName, String columnName, long upperBound) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.upperBound = upperBound;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getUpperBound() {
        return this.upperBound;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
