package org.evomaster.client.java.controller.internal.db.constraint;

import java.util.Arrays;
import java.util.List;

public class OrConstraint extends TableConstraint {

    private final List<TableConstraint> constraintList;

    public OrConstraint(String tableName, TableConstraint... constraints) {
        super(tableName);
        this.constraintList = Arrays.asList(constraints);
    }
}
