package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class JpaConstraint implements Serializable {

    private final String tableName;

    private final String columnName;

    private final  Boolean isNullable;

    private final  Boolean isOptional;

    private final  String minValue;

    private final  String maxValue;

    private final List<String> enumValuesAsStrings;

    public JpaConstraint(String tableName, String columnName, Boolean isNullable, Boolean isOptional, String minValue, String maxValue, List<String> enumValuesAsStrings) {
        this.tableName = Objects.requireNonNull(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.isNullable = isNullable;
        this.isOptional = isOptional;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.enumValuesAsStrings = enumValuesAsStrings;
    }

    public boolean isMeaningful(){
        return isNullable!=null || isOptional!=null || minValue!=null || maxValue!=null
                || (enumValuesAsStrings != null && ! enumValuesAsStrings.isEmpty());
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Boolean getNullable() {
        return isNullable;
    }

    public Boolean getOptional() {
        return isOptional;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public List<String> getEnumValuesAsStrings() {
        return enumValuesAsStrings;
    }
}
