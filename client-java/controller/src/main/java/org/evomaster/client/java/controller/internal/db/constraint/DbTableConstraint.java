package org.evomaster.client.java.controller.internal.db.constraint;

public abstract class DbTableConstraint {

    private final /*non-null*/ String tableName;

    public DbTableConstraint(String tableName) {
        if (tableName == null) {
            throw new IllegalArgumentException("table name cannot be null");
        }
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }
}
