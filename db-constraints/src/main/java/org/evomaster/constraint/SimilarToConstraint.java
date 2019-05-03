package org.evomaster.constraint;

public class SimilarToConstraint extends TableConstraint {

    private final /* non-null*/ String columnName;
    private final /*non-null*/ String pattern;

    public SimilarToConstraint(String tableName, String columnName, String pattern) {
        super(tableName);
        if (columnName == null) {
            throw new IllegalArgumentException("column name cannot be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }
        this.columnName = columnName;
        this.pattern = pattern;
    }
}
