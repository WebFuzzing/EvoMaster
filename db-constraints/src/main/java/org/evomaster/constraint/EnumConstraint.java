package org.evomaster.constraint;

import java.util.List;

public class EnumConstraint extends TableConstraint {

    private final List<String> valuesAsStrings;

    private final String columnName;

    public EnumConstraint(String tableName, String columnName, List<String> valuesAsStrings) {
        super(tableName);
        this.columnName = columnName;
        this.valuesAsStrings = valuesAsStrings;
    }

    public String getColumnName() {
        return columnName;
    }

    public List<String> getValuesAsStrings() {
        return this.valuesAsStrings;
    }
}
