package org.evomaster.client.java.sql.internal.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DbTableUniqueConstraint extends DbTableConstraint {

    private final /*non-null*/ List<String> uniqueColumnNames;

    public DbTableUniqueConstraint(String tableName, List<String> uniqueColumnNames) {
        super(tableName);
        Objects.requireNonNull(uniqueColumnNames);
        this.uniqueColumnNames = new ArrayList<>(uniqueColumnNames);
    }

    public List<String> getUniqueColumnNames() {
        return uniqueColumnNames;
    }
}
