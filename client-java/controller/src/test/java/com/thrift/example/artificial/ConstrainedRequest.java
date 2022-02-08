package com.thrift.example.artificial;

import javax.validation.constraints.*;
import java.util.List;

public class ConstrainedRequest {

    @NotEmpty@NotNull
    public List<String> list;

    @Max(100)@Min(0)
    public int intWithMinMax;

    @Max(1000L)@Min(-100L)
    public long longWithMinMax;

    @NotBlank@NotNull
    public String notBlankString;

    public String nullableString;

    @Size(min=2, max = 10)
    public String stringSize;

    @Size(min= 1, max = 10)
    public List<Integer> listSize;

    @CustomAnnotation(name = "kind", necessity = Necessity.REQUIRED)
    public EnumKind kind;

    //TODO eg, @DecimalMin(value = "0.1", inclusive = false)

    /*
        all more example based on
        https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
     */
}
