package org.evomaster.client.java.instrumentation.heuristic.validator.javax;

import javax.validation.constraints.*;

public class IntNullableBean {


    @Min(42)
    public Integer a;

    @Max(666)
    public Integer b;

    @Min(-5) @Max(-2)
    public Integer c;

    @Positive
    public Integer d;

    @PositiveOrZero
    public Integer e;

    @Negative
    public Integer f;

    @NegativeOrZero
    public Integer g;
}
