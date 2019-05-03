package org.evomaster.constraint;

import java.util.List;

public class UniqueConstraint extends TableConstraint {

    private final List<String> uniqueColumnNames;

    public UniqueConstraint(String tableName, List<String> uniqueColumnNames) {
        super(tableName);
        this.uniqueColumnNames = uniqueColumnNames;
    }

    public List<String> getUniqueColumnNames() {
        return uniqueColumnNames;
    }
}
