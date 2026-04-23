package org.evomaster.dbconstraint;

import java.util.Objects;

public abstract class TableConstraint {

    private final /*non-null*/ String tableName;


    public TableConstraint(String tableName) {
        this.tableName = Objects.requireNonNull(tableName);
    }

    public String getTableName() {
        return tableName;
    }

    public abstract <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument);

}
