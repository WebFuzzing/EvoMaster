package org.evomaster.dbconstraint;

import java.util.Objects;

public class LikeConstraint extends TableConstraint {

    private final /*non-null*/ String columnName;

    private final /*non-null*/ String pattern;

    public LikeConstraint(String tableName, String columnName, String pattern) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.pattern = Objects.requireNonNull(pattern);
    }
}
