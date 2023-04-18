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

    /**
     * Represents the minimum value (@Min) allowed for a field of an entity. The following types are supported:
     * <ul>
     * <li>BigDecimal</li>
     * <li>BigInteger</li>
     * <li>byte, short, int, long, and their respective wrappers</li>
     * </ul>
     * Note that double and float are not supported due to rounding errors (some providers might provide some approximative support).
     * For more information, see <a href="https://docs.oracle.com/javaee/7/api/javax/validation/constraints/Min.html">the official documentation</a>.
     */
    public Long minValue = null;

    /**

     Represents the maximum value (@Max) allowed for a field of an entity. The following types are supported:
     <ul>
     <li>BigDecimal</li>
     <li>BigInteger</li>
     <li>byte, short, int, long, and their respective wrappers</li>
     </ul>
     Note that double and float are not supported due to rounding errors (some providers might provide some approximative support).
     For more information, see <a href="https://docs.oracle.com/javaee/7/api/javax/validation/constraints/Max.html">the official documentation</a>.
     */
    public Long maxValue = null;

    public List<String> enumValuesAsStrings;

    /**
     * Contains the value of @DecimalMin(value=String) for an annotated element of an entity.
     * Supported types are:
     * - BigDecimal
     * - BigInteger
     * - CharSequence
     * - byte, short, int, long, and their respective wrappers
     * Note that double and float are not supported due to rounding errors (some providers might provide some approximative support).
     * null elements are considered valid.
     * <a href="https://docs.oracle.com/javaee/7/api/javax/validation/constraints/DecimalMin.html">See official documentation</a>
     */
    public String decimalMinValue;


    /**
     * Contains the value of @DecimalMax(value=String) for an annotated element of an entity.
     * Supported types are:
     * - BigDecimal
     * - BigInteger
     * - CharSequence
     * - byte, short, int, long, and their respective wrappers
     * Note that double and float are not supported due to rounding errors (some providers might provide some approximative support).
     * null elements are considered valid.
     * <a href="https://docs.oracle.com/javaee/7/api/javax/validation/constraints/DecimalMax.html">Official documentation</a>
     */
    public String decimalMaxValue;

    /**
     * Indicates that the element contains a @NotBlank annotation.
     * The annotated element must not be null and must contain at least one non-whitespace character.
     * Accepts CharSequence.
     * <a href="https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/NotBlank.html">Official documentation</a>
     */
    public Boolean isNotBlank;
    /**
     * Indicates the element contains a @Email annotaion
     * The string has to be a well-formed email address. Accepts CharSequence.
     * <a href="https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Email.html">Official documentation</a>
     */
    public Boolean isEmail;

    /**
     * The element contains a @Negative annotation.
     * Supported types are:
     * - BigDecimal
     * - BigInteger
     * - byte, short, int, long, float, double and their respective wrappers.
     * null elements are considered valid. E.g. if a field foo is annotated @Negative,
     * the value of foo can be null.
     * <a href="https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Negative.html">Official documentation</a>
     */
    public Boolean isNegative;

    /**
     * The element contains a @NegativeOrZero annotation.
     * Supported types are:
     * - BigDecimal
     * - BigInteger
     * - byte, short, int, long, float, double and their respective wrappers.
     * null elements are considered valid. E.g. if a field foo is annotated @NegativeOrZero,
     * the value of foo can be null.
     * <a href="https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/NegativeOrZero.html">See official documentation</a>
     */
    public Boolean isNegativeOrZero;

    /**
     * The element contains a @Positive annotation.
     * Supported types are:
     * - BigDecimal
     * - BigInteger
     * - byte, short, int, long, float, double and their respective wrappers.
     * null elements are considered valid. E.g. if a field foo is annotated @Positive,
     * the value of foo can be null.
     * <a href="https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Positive.html">Official documentation</a>
     */
    public Boolean isPositive;
    /**
     * The element contains a @PositiveOrZero annotation.
     * Supported types are:
     * - BigDecimal
     * - BigInteger
     * - byte, short, int, long, float, double and their respective wrappers.
     * null elements are considered valid. E.g. if a field foo is annotated @PositiveOrZero,
     * the value of foo can be null.
     * <a href="https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/PositiveOrZero.html">Official documentation</a>
     */
    public Boolean isPositiveOrZero;

    public Boolean isFuture;
    public Boolean isFutureOrPresent;
    public Boolean isPast;
    public Boolean isPastOrPresent;
    public Boolean isAlwaysNull;
    /**
     * Stores the string value of a @Pattern(regexp=string) annotation for a given element.
     * The regular expression follows the Java regular expression conventions see Pattern.
     * null elements are considered valid. If a field foo contains the @Pattern(regexp=string)
     * annotation, foo can be null (unless explicitly @NotNull).
     * Accepts CharSequence.
     *
     */
    public String patternRegExp;
    public Integer sizeMin;
    public Integer sizeMax;
    public Integer digitsInteger;
    public Integer digitsFraction;

    //TODO much more can be added here

}
