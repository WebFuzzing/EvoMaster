package org.evomaster.dbconstraint.extract;

import org.evomaster.dbconstraint.ConstraintDatabaseType;

import java.util.Objects;

public class TranslationContext {

    private final String currentTableName;

    private final /*non-null*/ ConstraintDatabaseType databaseType;

    public TranslationContext(String currentTableName, ConstraintDatabaseType databaseType) {
        this.currentTableName = currentTableName;
        this.databaseType = Objects.requireNonNull(databaseType);
    }

    public String getCurrentTableName() {
        return currentTableName;
    }

    public ConstraintDatabaseType getDatabaseType() {
        return databaseType;
    }
}
