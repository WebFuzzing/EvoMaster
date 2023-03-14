package org.evomaster.client.java.controller.api.dto.constraint;


import java.util.List;

/**
 * Set of possible constraints applicable to a single element (ie, no intra-dependency constraints among
 * different elements).
 * <p>
 * Note: the type and id of the element is undefined here.
 * Some constraint might be interpreted differently based on the type of the element.
 * For example, a min value could be an integer or a double based on the element.
 * This also implies that numeric values are passed as string (this also helps with JSON representation issues)
 */
public class ElementConstraintsDto {

    public Boolean isNullable = null;

    public Boolean isOptional = null;

    public Long minValue = null;

    public Long maxValue = null;

    public List<String> enumValuesAsStrings;

    public String decimalMinValue;
    public String decimalMaxValue;

    public Boolean isNotBlank;
    public Boolean isEmail;
    public Boolean isNegative;
    public Boolean isNegativeOrZero;

    public Boolean isPositive;
    public Boolean isPositiveOrZero;
    public Boolean isFuture;
    public Boolean isFutureOrPresent;
    public Boolean isPast;
    public Boolean isPastOrPresent;
    public Boolean isAlwaysNull;
    public String patternRegExp;
    public Integer sizeMin;
    public Integer sizeMax;
    public Integer digitsInteger;
    public Integer digitsFraction;

    //TODO much more can be added here

}
