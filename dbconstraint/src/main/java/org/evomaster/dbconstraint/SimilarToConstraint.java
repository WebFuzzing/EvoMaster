package org.evomaster.dbconstraint;

import java.util.Objects;

public class SimilarToConstraint extends TableConstraint {

    private final /* non-null*/ String columnName;

    private final /*non-null*/ String pattern;

    private final /*non-null*/ ConstraintDatabaseType databaseType;

    public SimilarToConstraint(String tableName, String columnName, String pattern, ConstraintDatabaseType databaseType) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.pattern = Objects.requireNonNull(pattern);
        this.databaseType = Objects.requireNonNull(databaseType);
    }

    public String getColumnName() {
        return columnName;
    }

    public String getPattern() {
        return pattern;
    }

    public ConstraintDatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
