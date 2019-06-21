package org.evomaster.dbconstraint;

import java.util.Objects;

public class SimilarToConstraint extends TableConstraint {

    private final /* non-null*/ String columnName;
    private final /*non-null*/ String pattern;

    public SimilarToConstraint(String tableName, String columnName, String pattern) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.pattern = Objects.requireNonNull(pattern);
    }

    public String getColumnName() {
        return columnName;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
