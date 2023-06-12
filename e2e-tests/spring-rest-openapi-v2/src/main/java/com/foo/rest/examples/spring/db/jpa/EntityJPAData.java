package com.foo.rest.examples.spring.db.jpa;


import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ExistingTable")
public class EntityJPAData {


    private @Id int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    @Min(value = 42)
    @Max(value = 45)
    private int x1;

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }


    @NotBlank
    @Column(name = "notblank")
    private String notblank;

    public String getNotBlank() {
        return notblank;
    }

    public void setNotBlank(String notBlank) {
        this.notblank = notBlank;
    }

    @Email
    @NotNull
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Negative
    private int negative;

    public int getNegative() {
        return negative;
    }

    public void setNegative(int negative) {
        this.negative = negative;
    }

    @NegativeOrZero
    @Column(name = "negative_or_zero")
    private int negativeOrZero;

    public int getNegativeOrZero() {
        return negativeOrZero;
    }

    public void setNegativeOrZero(int negativeOrZero) {
        this.negativeOrZero = negativeOrZero;
    }

    @Positive
    private int positive;

    public int getPositive() {
        return positive;
    }

    public void setPositive(int positive) {
        this.positive = positive;
    }

    @PositiveOrZero
    @Column(name = "positive_or_zero")
    private int positiveOrZero;

    public int getPositiveOrZero() {
        return positiveOrZero;
    }

    public void setPositiveOrZero(int positiveOrZero) {
        this.positiveOrZero = positiveOrZero;
    }

    @DecimalMin(value = "3.1416")
    @Column(name = "decimal_min")
    private int decimalMin;

    public int getDecimalMin() {
        return this.decimalMin;
    }

    public void setDecimalMin(int decimalMin) {
        this.decimalMin = decimalMin;
    }

    @DecimalMax(value = "0.000001")
    @Column(name = "decimal_max")
    private int decimalMax;

    public int getDecimalMax() {
        return this.decimalMax;
    }

    public void setDecimalMax(int decimalMax) {
        this.decimalMax = decimalMax;
    }

    @Size(min = 1, max = 8)
    @Column(name = "size")
    private String size;

    public String getSize() {
        return this.size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Pattern(regexp="\\Qfoo.com\\E")
    @Column(name = "reg_exp")
    private String regexp;

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }


    @Digits(integer = 2, fraction = 3)
    @Column(name = "big_decimal")
    private BigDecimal bigDecimal;
}
