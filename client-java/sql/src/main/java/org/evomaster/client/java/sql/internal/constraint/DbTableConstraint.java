package org.evomaster.client.java.sql.internal.constraint;

import java.util.Objects;

public abstract class DbTableConstraint {

    private final /*non-null*/ String tableName;

    public DbTableConstraint(String tableName) {
        this.tableName = Objects.requireNonNull(tableName);
    }

    public String getTableName() {
        return tableName;
    }
}
