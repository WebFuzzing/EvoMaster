package com.thrift.example.artificial;

import javax.validation.constraints.*;
import java.util.List;

public class ConstrainedRequest {

    @NotEmpty
    public List<String> list;

    @Max(100)@Min(0)
    public int intWithMinMax;

    @Max(1000L)@Min(-100L)
    public long longWithMinMax;

    @NotBlank
    public String notBlankString;

    public String nullableString;

    @Size(min=2, max = 10)
    public String stringSize;

    @NotEmpty
    @Size(min= 1, max = 10)
    public List<Integer> listSize;

    @CustomAnnotation(name = "kind", necessity = Necessity.REQUIRED)
    public EnumKind kind;

    @Pattern(regexp = "\\d{4}-\\d{1,2}-\\d{1,2}")
    public String date;

    @DecimalMax(value = "10")
    @DecimalMin(value = "1")
    public long longWithDecimalMinMax;

    @DecimalMax(value = "10", inclusive = false)
    @DecimalMin(value = "1", inclusive = false)
    public Long longWithInclusiveFDecimalMainMax;

    /*
        all more example based on
        https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
     */
}
