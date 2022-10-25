package org.evomaster.client.java.instrumentation;

import java.util.Objects;

public class JpaConstraint {

    private final String tableName;

    private final String columnName;

    private final  Boolean isNullable;

    private final  Boolean isOptional;

    private final  String minValue;

    private final  String maxValue;


    public JpaConstraint(String tableName, String columnName, Boolean isNullable, Boolean isOptional, String minValue, String maxValue) {
        this.tableName = Objects.requireNonNull(tableName);
        this.columnName = Objects.requireNonNull(columnName);
        this.isNullable = isNullable;
        this.isOptional = isOptional;
        this.minValue = minValue;
        this.maxValue = maxValue;
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
}
