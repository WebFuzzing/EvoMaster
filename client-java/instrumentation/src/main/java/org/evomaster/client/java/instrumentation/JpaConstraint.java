package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class JpaConstraint implements Serializable {

    private final String tableName;

    private final String columnName;

    /**
     * Correspond to the @NotNull annotation.
     * If the annotation is present, then isNullable must be false.
     */
    private final Boolean isNullable;

    private final Boolean isOptional;

    /**
     * Correspond to the @Min(value=Long) annotation
     */
    private final Long minValue;

    /**
     * Correspond to the @Max(value=Long) and @DecimalMax annotations
     */
    private final Long maxValue;


    private final List<String> enumValuesAsStrings;


    /**
     * Correspond to the @DecimalMin(value=String) annotation
     */
    private final String decimalMinValue;


    /**
     * Correspond to the @DecimalMax(value=String)
     */
    private final String decimalMaxValue;

    /**
     * Correspond to the @NotBlank annotation on String columns.
     * If @NotBlank is found, the isNotBlank must be true,
     * otherwise isNotBlank will be null.
     */
    private final Boolean isNotBlank;

    /**
     * Correspond to the @Email annotation on String columns.
     * If @Email is found, the isEmail must be true,
     * otherwise isNotBlank will be null.
     */
    private final Boolean isEmail;


    /**
     * Correspond to the @Negative annotation on numerical columns.
     * If @Negative is found, the isNegative must be true,
     * otherwise isNegative will be null.
     */
    private final Boolean isNegative;

    /**
     * Correspond to the @NegativeOrZero annotation on numerical columns.
     * If @NegativeOrZero is found, the isNegativeOrZero must be true,
     * otherwise isNegativeOrZero will be null.
     */
    private final Boolean isNegativeOrZero;

    /**
     * Correspond to the @Positive annotation on numerical columns.
     * If @Positive is found, the isPositve must be true,
     * otherwise isPositive will be null.
     */
    private final Boolean isPositive;

    /**
     * Correspond to the @PositiveOrZero annotation on numerical columns.
     * If @PositiveOrZero is found, the isPositiveOrZero must be true,
     * otherwise isPositiveOrZero will be null.
     */
    private final Boolean isPositiveOrZero;

    /**
     * Correspond to the @Future annotation on numerical columns.
     * If @Future is found, the isFuture must be true,
     * otherwise isFuture will be null.
     */
    private final Boolean isFuture;

    /**
     * Correspond to the @FutureOrPresent annotation on numerical columns.
     * If @FutureOrPresent is found, the isFutureOrPresent must be true,
     * otherwise isFutureOrPresent will be null.
     */
    private final Boolean isFutureOrPresent;

    /**
     * Correspond to the @Past annotation on date and time columns
     */
    private final Boolean isPast;

    /**
     * Correspond to the @PastOrPresent annotation on date and time column
     */
    private final Boolean isPastOrPresent;

    /**
     * Correspond to the @Null annotation on nullable columns
     */
    private final Boolean isAlwaysNull;

    /**
     * Correspond to the value of the @Pattern(regex=String) annotation on a column
     */
    private final String patternRegExp;

    /**
     * Correspond to the min element of the @Size(min=int,max=int) annotation
     */
    private final Integer sizeMin;

    /**
     * Correspond to the min element of the @Size(min=int,max=int) annotation
     */
    private final Integer sizeMax;

    /**
     * Correspond to the fraction element of the @Digits(integer=int,fraction=int) annotation
     */
    private final Integer digitsInteger;

    /**
     * Correspond to the fraction element of the @Digits(integer=int,fraction=int) annotation
     */
    private final Integer digitsFraction;


    public JpaConstraint(String tableName,
                         String columnName,
                         Boolean isNullable,
                         Boolean isOptional,
                         Long minValue,
                         Long maxValue,
                         List<String> enumValuesAsStrings,
                         String decimalMinValue,
                         String decimalMaxValue,
                         Boolean isNotBlank,
                         Boolean isEmail,
                         Boolean isNegative,
                         Boolean isNegativeOrZero,
                         Boolean isPositive,
                         Boolean isPositiveOrZero,
                         Boolean isFuture,
                         Boolean isFutureOrPresent,
                         Boolean isPast,
                         Boolean isPastOrPresent,
                         Boolean isAlwaysNull,
                         String patternRegExp,
                         Integer sizeMin,
                         Integer sizeMax,
                         Integer digitsInteger,
                         Integer digitsFraction
    ) {

        this.tableName = Objects.requireNonNull(tableName);
        this.columnName = Objects.requireNonNull(columnName);

        this.isNullable = isNullable;
        this.isOptional = isOptional;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.enumValuesAsStrings = enumValuesAsStrings;
        this.decimalMinValue = decimalMinValue;
        this.decimalMaxValue = decimalMaxValue;
        this.isNotBlank = isNotBlank;
        this.isEmail = isEmail;
        this.isNegative = isNegative;
        this.isNegativeOrZero = isNegativeOrZero;
        this.isPositive = isPositive;
        this.isPositiveOrZero = isPositiveOrZero;
        this.isFuture = isFuture;
        this.isFutureOrPresent = isFutureOrPresent;
        this.isPast = isPast;
        this.isPastOrPresent = isPastOrPresent;
        this.isAlwaysNull = isAlwaysNull;
        this.patternRegExp = patternRegExp;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.digitsInteger = digitsInteger;
        this.digitsFraction = digitsFraction;
    }

    public boolean isMeaningful() {
        return isNullable != null
                || isOptional != null
                || minValue != null
                || maxValue != null
                || (enumValuesAsStrings != null && !enumValuesAsStrings.isEmpty())
                || decimalMinValue != null
                || decimalMaxValue != null
                || isNotBlank != null
                || isEmail != null
                || isNegative != null
                || isNegativeOrZero != null
                || isPositive != null
                || isPositiveOrZero != null
                || isFuture != null
                || isFutureOrPresent != null
                || isPast != null
                || isPastOrPresent != null
                || isAlwaysNull != null
                || patternRegExp != null
                || sizeMin != null
                || sizeMax != null
                || digitsInteger != null
                || digitsFraction != null;
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

    public Long getMinValue() {
        return minValue;
    }

    public Long getMaxValue() {
        return maxValue;
    }

    public List<String> getEnumValuesAsStrings() {
        return enumValuesAsStrings;
    }

    public Boolean getNotBlank() {
        return isNotBlank;
    }

    public Boolean getIsEmail() {
        return isEmail;
    }

    public Boolean getIsPositive() {
        return isPositive;
    }

    public Boolean getIsPositiveOrZero() {
        return isPositiveOrZero;
    }

    public Boolean getIsNegative() {
        return isNegative;
    }

    public Boolean getIsNegativeOrZero() {
        return isNegativeOrZero;
    }

    public Boolean getIsPast() {
        return isPast;
    }

    public Boolean getIsPastOrPresent() {
        return isPastOrPresent;
    }

    public Boolean getIsFuture() {
        return isFuture;
    }

    public Boolean getIsFutureOrPresent() {
        return isFutureOrPresent;
    }

    public Boolean getIsAlwaysNull() {
        return isAlwaysNull;
    }

    public String getDecimalMinValue() {
        return decimalMinValue;
    }

    public String getDecimalMaxValue() {
        return decimalMaxValue;
    }

    public String getPatternRegExp() {
        return patternRegExp;
    }

    public Integer getSizeMin() {
        return sizeMin;
    }

    public Integer getSizeMax() {
        return sizeMax;
    }

    public Integer getDigitsFraction() {
        return digitsFraction;
    }

    public Integer getDigitsInteger() {
        return digitsInteger;
    }
}
