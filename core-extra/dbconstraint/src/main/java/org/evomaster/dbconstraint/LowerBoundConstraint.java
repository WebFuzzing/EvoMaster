package org.evomaster.dbconstraint;

import java.util.Objects;

/**
 * Represents the constraint value <= table.column
 */
public class LowerBoundConstraint extends TableConstraint {

    private final /*non-null*/ String columnName;

    private final long lowerBound;

    public LowerBoundConstraint(String tableName, String columnName, long lowerBound) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.lowerBound = lowerBound;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public long getLowerBound() {
        return lowerBound;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
