package org.evomaster.client.java.controller.internal.db.constraint;

import java.util.List;

public class DbTableUniqueConstraint extends DbTableConstraint {

    private final List<String> uniqueColumnNames;

    public DbTableUniqueConstraint(String tableName, List<String> uniqueColumnNames) {
        super(tableName);
        this.uniqueColumnNames = uniqueColumnNames;
    }

    public List<String> getUniqueColumnNames() {
        return uniqueColumnNames;
    }
}
