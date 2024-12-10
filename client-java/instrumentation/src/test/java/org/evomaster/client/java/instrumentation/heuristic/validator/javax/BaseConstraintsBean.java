package org.evomaster.client.java.instrumentation.heuristic.validator.javax;


import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class BaseConstraintsBean {

    @NotNull
    public Object fooNotNull;

    public Object foo;

    @Min(42)
    public int x;

    @Max(666)
    public int y;

    @Min(1) @Max(16)
    public int z;
}
