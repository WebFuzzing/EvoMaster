package org.evomaster.dbconstraint;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LikeConstraint extends TableConstraint {

    private final /*non-null*/ String columnName;

    /**
     * A non-empty list of non-null string values
     */
    private final /*non-null*/ List<String> patterns;

    private final /*non-null*/ ConstraintDatabaseType databaseType;

    public LikeConstraint(String tableName, String columnName, String pattern, ConstraintDatabaseType databaseType) {
        this(tableName, columnName, Collections.singletonList(pattern), databaseType);
    }

    public LikeConstraint(String tableName, String columnName, List<String> patterns, ConstraintDatabaseType databaseType) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.patterns = Objects.requireNonNull(patterns);
        if (this.patterns.stream().anyMatch(p -> p == null)) {
            throw new NullPointerException();
        }
        if (this.patterns.isEmpty()) {
            throw new IllegalArgumentException("list of patterns cannot be null");
        }
        this.databaseType = Objects.requireNonNull(databaseType);
    }


    public String getColumnName() {
        return columnName;
    }

    public String getPattern() {
        if (patterns.size() != 1) {
            throw new IllegalStateException("Cannot get a single pattern of a multi-pattern like constraint");
        }
        return patterns.get(0);
    }


    public List<String> getPatterns() {
        return patterns;
    }

    public ConstraintDatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
