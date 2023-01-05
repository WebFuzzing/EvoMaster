package org.evomaster.client.java.instrumentation.heuristic.validator.javax;

import javax.validation.constraints.*;

public class IntBean {

    @Min(42)
    public int a;

    @Max(666)
    public int b;

    @Min(-5) @Max(-2)
    public int c;

    @Positive
    public int d;

    @PositiveOrZero
    public int e;

    @Negative
    public int f;

    @NegativeOrZero
    public int g;
}
