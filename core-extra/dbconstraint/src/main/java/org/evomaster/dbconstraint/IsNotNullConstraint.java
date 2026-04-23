package org.evomaster.dbconstraint;

import java.util.Objects;

public class IsNotNullConstraint extends TableConstraint {

    private final /*non-null*/ String columnName;

    public IsNotNullConstraint(String tableName, String columnName) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

}
