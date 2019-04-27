package org.evomaster.client.java.controller.internal.db.constraint;

import java.util.List;

public class EnumConstraint extends SchemaConstraint {

    private final List<String> valuesAsStrings;

    private final String columnName;

    public EnumConstraint(String columnName, List<String> valuesAsStrings) {
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
