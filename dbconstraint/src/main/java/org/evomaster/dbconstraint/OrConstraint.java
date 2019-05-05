package org.evomaster.dbconstraint;


import java.util.Arrays;
import java.util.List;

public class OrConstraint extends TableConstraint {

    private final /*non-null*/ List<TableConstraint> constraintList;

    public OrConstraint(String tableName, TableConstraint... constraints) {
        super(tableName);
        this.constraintList = Arrays.asList(constraints);
    }
}
