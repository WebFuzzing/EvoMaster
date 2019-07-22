package org.evomaster.dbconstraint;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class IffConstraint extends TableConstraint {


    private final /*non-null*/ TableConstraint left;

    private final /*non-null*/ TableConstraint right;

    public IffConstraint(String tableName, TableConstraint left, TableConstraint right) {
        super(tableName);
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public TableConstraint getLeft() {
        return left;
    }

    public TableConstraint getRight() {
        return right;
    }

    public List<TableConstraint> getConstraintList() {
        return Arrays.asList(left, right);
    }
}
