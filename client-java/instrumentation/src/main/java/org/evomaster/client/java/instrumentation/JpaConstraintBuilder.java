package org.evomaster.client.java.instrumentation;

import java.util.List;

public class JpaConstraintBuilder {
    private String tableName;
    private String columnName;
    private Boolean isNullable;
    private Boolean isOptional;
    private Long minValue;
    private Long maxValue;
    private List<String> enumValuesAsStrings;
    private String decimalMinValue;
    private String decimalMaxValue;
    private Boolean isNotBlank;
    private Boolean isEmail;
    private Boolean isNegative;
    private Boolean isNegativeOrZero;
    private Boolean isPositive;
    private Boolean isPositiveOrZero;
    private Boolean isFuture;
    private Boolean isFutureOrPresent;
    private Boolean isPast;
    private Boolean isPastOrPresent;
    private Boolean isAlwaysNull;
    private String patternRegExp;
    private Integer sizeMin;
    private Integer sizeMax;
    private Integer digitsInteger;
    private Integer digitsFraction;

    public JpaConstraintBuilder withTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public JpaConstraintBuilder withColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public JpaConstraintBuilder withIsNullable(Boolean isNullable) {
        this.isNullable = isNullable;
        return this;
    }

    public JpaConstraintBuilder withIsOptional(Boolean isOptional) {
        this.isOptional = isOptional;
        return this;
    }

    public JpaConstraintBuilder withMinValue(Long minValue) {
        this.minValue = minValue;
        return this;
    }

    public JpaConstraintBuilder withMaxValue(Long maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public JpaConstraintBuilder withEnumValuesAsStrings(List<String> enumValuesAsStrings) {
        this.enumValuesAsStrings = enumValuesAsStrings;
        return this;
    }

    public JpaConstraintBuilder withDecimalMinValue(String decimalMinValue) {
        this.decimalMinValue = decimalMinValue;
        return this;
    }

    public JpaConstraintBuilder withDecimalMaxValue(String decimalMaxValue) {
        this.decimalMaxValue = decimalMaxValue;
        return this;
    }

    public JpaConstraintBuilder withIsNotBlank(Boolean isNotBlank) {
        this.isNotBlank = isNotBlank;
        return this;
    }

    public JpaConstraintBuilder withIsEmail(Boolean isEmail) {
        this.isEmail = isEmail;
        return this;
    }

    public JpaConstraintBuilder withIsNegative(Boolean isNegative) {
        this.isNegative = isNegative;
        return this;
    }

    public JpaConstraintBuilder withIsNegativeOrZero(Boolean isNegativeOrZero) {
        this.isNegativeOrZero = isNegativeOrZero;
        return this;
    }

    public JpaConstraintBuilder withIsPositive(Boolean isPositive) {
        this.isPositive = isPositive;
        return this;
    }

    public JpaConstraintBuilder withIsPositiveOrZero(Boolean isPositiveOrZero) {
        this.isPositiveOrZero = isPositiveOrZero;
        return this;
    }

    public JpaConstraintBuilder withIsFuture(Boolean isFuture) {
        this.isFuture = isFuture;
        return this;
    }

    public JpaConstraintBuilder withIsFutureOrPresent(Boolean isFutureOrPresent) {
        this.isFutureOrPresent = isFutureOrPresent;
        return this;
    }

    public JpaConstraintBuilder withIsPast(Boolean isPast) {
        this.isPast = isPast;
        return this;
    }

    public JpaConstraintBuilder withIsPastOrPresent(Boolean isPastOrPresent) {
        this.isPastOrPresent = isPastOrPresent;
        return this;
    }

    public JpaConstraintBuilder withIsAlwaysNull(Boolean isAlwaysNull) {
        this.isAlwaysNull = isAlwaysNull;
        return this;
    }

    public JpaConstraintBuilder withPatternRegExp(String patternRegExp) {
        this.patternRegExp = patternRegExp;
        return this;
    }

    public JpaConstraintBuilder withSizeMin(Integer sizeMin) {
        this.sizeMin = sizeMin;
        return this;
    }

    public JpaConstraintBuilder withSizeMax(Integer sizeMax) {
        this.sizeMax = sizeMax;
        return this;
    }

    public JpaConstraintBuilder withDigitsInteger(Integer digitsInteger) {
        this.digitsInteger = digitsInteger;
        return this;
    }

    public JpaConstraintBuilder withDigitsFraction(Integer digitsFraction) {
        this.digitsFraction = digitsFraction;
        return this;
    }

    public JpaConstraint createJpaConstraint() {
        return new JpaConstraint(tableName, columnName, isNullable, isOptional, minValue, maxValue, enumValuesAsStrings,
                decimalMinValue, decimalMaxValue, isNotBlank, isEmail, isNegative, isNegativeOrZero, isPositive,
                isPositiveOrZero, isFuture, isFutureOrPresent, isPast, isPastOrPresent, isAlwaysNull, patternRegExp,
                sizeMin, sizeMax, digitsInteger, digitsFraction);
    }
}