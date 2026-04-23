package org.evomaster.dbconstraint;

import java.util.List;
import java.util.Objects;

public class EnumConstraint extends TableConstraint {

    private final /*non-null*/ List<String> valuesAsStrings;

    private final /*non-null*/ String columnName;

    public EnumConstraint(String tableName, String columnName, List<String> valuesAsStrings) {
        super(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.valuesAsStrings = Objects.requireNonNull(valuesAsStrings);
    }

    public String getColumnName() {
        return columnName;
    }

    public List<String> getValuesAsStrings() {
        return this.valuesAsStrings;
    }

    @Override
    public <K, V> K accept(TableConstraintVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
