package org.evomaster.constraint;

public abstract class TableConstraint {

    private final /*non-null*/ String tableName;


    public TableConstraint(String tableName) {
        if (tableName == null) {
            throw new IllegalArgumentException("table name cannot be null");
        }
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }


}
