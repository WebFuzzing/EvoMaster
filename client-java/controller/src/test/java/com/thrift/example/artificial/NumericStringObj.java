package com.thrift.example.artificial;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;

public class NumericStringObj {

    @DecimalMax(value = ""+Long.MAX_VALUE, inclusive = false)
    private String longValue;

    @DecimalMax(value = ""+Integer.MAX_VALUE, inclusive = false)
    @NotBlank
    private String intValue;

    @DecimalMax(value = ""+Long.MAX_VALUE, inclusive = false)
    private String bigIntegerValue;

    @DecimalMax(value = ""+Double.MAX_VALUE, inclusive = false)
    @Digits(integer = 10, fraction = 5)
    @DecimalMin(value =  "0", inclusive = false)
    private String bigDecimalValue;

    public String getLongValue() {
        return longValue;
    }

    public void setLongValue(String longValue) {
        this.longValue = longValue;
    }

    public String getIntValue() {
        return intValue;
    }

    public void setIntValue(String intValue) {
        this.intValue = intValue;
    }

    public String getBigIntegerValue() {
        return bigIntegerValue;
    }

    public void setBigIntegerValue(String bigIntegerValue) {
        this.bigIntegerValue = bigIntegerValue;
    }

    public String getBigDecimalValue() {
        return bigDecimalValue;
    }

    public void setBigDecimalValue(String bigDecimalValue) {
        this.bigDecimalValue = bigDecimalValue;
    }

    @Override
    public String toString() {
        return "NumericStringObj{" +
                "longValue='" + longValue + '\'' +
                ", intValue='" + intValue + '\'' +
                ", bigIntegerValue='" + bigIntegerValue + '\'' +
                ", bigDecimalValue='" + bigDecimalValue + '\'' +
                '}';
    }
}
