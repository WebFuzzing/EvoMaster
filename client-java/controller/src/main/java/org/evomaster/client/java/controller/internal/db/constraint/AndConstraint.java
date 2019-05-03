package org.evomaster.client.java.controller.internal.db.constraint;

public class AndConstraint extends TableConstraint {

    private final TableConstraint left;

    private final TableConstraint right;

    public AndConstraint(String tableName, TableConstraint left, TableConstraint right) {
        super(tableName);
        this.left = left;
        this.right = right;
    }
}
