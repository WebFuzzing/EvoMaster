package org.evomaster.constraint;

public class LikeConstraint extends TableConstraint {
    private final String columnName;

    private final String pattern;

    public LikeConstraint(String tableName, String columnName, String pattern) {
        super(tableName);
        this.columnName = columnName;
        this.pattern = pattern;
    }
}
