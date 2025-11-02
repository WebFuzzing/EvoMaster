package org.evomaster.dbconstraint;

import java.util.List;
import java.util.Objects;

public class UniqueConstraint extends TableConstraint {

    private final /*non-null*/ List<String> uniqueColumnNames;

    public UniqueConstraint(String tableName, List<String> uniqueColumnNames) {
        super(tableName);
        this.uniqueColumnNames = Objects.requireNonNull(uniqueColumnNames);
    }

    public List<String> getUniqueColumnNames() {
        return uniqueColumnNames;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
