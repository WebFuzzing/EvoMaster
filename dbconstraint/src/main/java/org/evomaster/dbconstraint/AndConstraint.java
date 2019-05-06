package org.evomaster.dbconstraint;

import java.util.Objects;

public class AndConstraint extends TableConstraint {

    private final /*non-null*/ TableConstraint left;

    private final /*non-null*/ TableConstraint right;

    public AndConstraint(String tableName, TableConstraint left, TableConstraint right) {
        super(tableName);
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }
}
