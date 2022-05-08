package com.thrift.example.artificial;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class BigNumberObj {

    // BigDecimal
    @DecimalMax("42.42")
    @Digits(integer = 2, fraction = 2)
    @Positive
    private BigDecimal bdPositiveFloat;

    @DecimalMin("-42.42")
    @Digits(integer = 2, fraction = 2)
    @Negative
    private BigDecimal bdNegativeFloat;


    @DecimalMax("42.42")
    @Digits(integer = 2, fraction = 2)
    @PositiveOrZero
    private BigDecimal bdPositiveOrZeroFloat;

    @DecimalMin("-42.42")
    @Digits(integer = 2, fraction = 2)
    @NegativeOrZero
    private BigDecimal bdNegativeOrZeroFloat;

    // BigInteger
    @Max(42)
    @Digits(integer = 2, fraction = 0)
    @Positive
    private BigInteger biPositive;

    @Max(42)
    @Digits(integer = 2, fraction = 0)
    @PositiveOrZero
    private BigInteger biPositiveOrZero;

    @Min(-42)
    @Digits(integer = 2, fraction = 0)
    @Negative
    private BigInteger biNegative;

    @Min(-42)
    @Digits(integer = 2, fraction = 0)
    @NegativeOrZero
    private BigInteger biNegativeOrZero;


    public BigDecimal getBdPositiveFloat() {
        return bdPositiveFloat;
    }

    public void setBdPositiveFloat(BigDecimal bdPositiveFloat) {
        this.bdPositiveFloat = bdPositiveFloat;
    }

    public BigDecimal getBdNegativeFloat() {
        return bdNegativeFloat;
    }

    public void setBdNegativeFloat(BigDecimal bdNegativeFloat) {
        this.bdNegativeFloat = bdNegativeFloat;
    }

    public BigDecimal getBdPositiveOrZeroFloat() {
        return bdPositiveOrZeroFloat;
    }

    public void setBdPositiveOrZeroFloat(BigDecimal bdPositiveOrZeroFloat) {
        this.bdPositiveOrZeroFloat = bdPositiveOrZeroFloat;
    }

    public BigDecimal getBdNegativeOrZeroFloat() {
        return bdNegativeOrZeroFloat;
    }

    public void setBdNegativeOrZeroFloat(BigDecimal bdNegativeOrZeroFloat) {
        this.bdNegativeOrZeroFloat = bdNegativeOrZeroFloat;
    }

    public BigInteger getBiPositive() {
        return biPositive;
    }

    public void setBiPositive(BigInteger biPositive) {
        this.biPositive = biPositive;
    }

    public BigInteger getBiPositiveOrZero() {
        return biPositiveOrZero;
    }

    public void setBiPositiveOrZero(BigInteger biPositiveOrZero) {
        this.biPositiveOrZero = biPositiveOrZero;
    }

    public BigInteger getBiNegative() {
        return biNegative;
    }

    public void setBiNegative(BigInteger biNegative) {
        this.biNegative = biNegative;
    }

    public BigInteger getBiNegativeOrZero() {
        return biNegativeOrZero;
    }

    public void setBiNegativeOrZero(BigInteger biNegativeOrZero) {
        this.biNegativeOrZero = biNegativeOrZero;
    }

    @Override
    public String toString() {
        return "BigNumberObj{" +
                "bdPositiveFloat=" + bdPositiveFloat +
                ", bdNegativeFloat=" + bdNegativeFloat +
                ", bdPositiveOrZeroFloat=" + bdPositiveOrZeroFloat +
                ", bdNegativeOrZeroFloat=" + bdNegativeOrZeroFloat +
                ", biPositive=" + biPositive +
                ", biPositiveOrZero=" + biPositiveOrZero +
                ", biNegative=" + biNegative +
                ", biNegativeOrZero=" + biNegativeOrZero +
                '}';
    }
}
