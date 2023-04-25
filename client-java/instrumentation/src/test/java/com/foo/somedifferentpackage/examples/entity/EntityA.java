package com.foo.somedifferentpackage.examples.entity;


import javax.persistence.Entity;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class EntityA {

    @Min(value = -1)
    public int minValueColumn;

    @Max(value = 200)
    public int maxValueColumn;

    @NotBlank
    public String notBlankColumn;

    @Email
    public String emailColumn;

    @Negative
    public int negativeColumn;

    @NegativeOrZero
    public int negativeOrZeroColumn;

    @Positive
    public int positiveColumn;

    @PositiveOrZero
    public int positiveOrZeroColumn;

    @Future
    public LocalDate futureColumn;

    @FutureOrPresent
    public LocalDate futureOrPresentColumn;

    @Past
    public LocalDate pastColumn;

    @PastOrPresent
    public LocalDate pastOrPresentColumn;

    @Null
    public Object nullColumn;

    @DecimalMin(value="-1.0")
    public Float decimalMinColumn;

    @DecimalMax(value="42.0")
    public Float decimalMaxColumn;

    @Pattern(regexp="[0-9]+")
    public String patternColumn;

    @Size(min=3)
    public List<Object> onlySizeMinColumn;

    @Size(max=10)
    public List<Object> onlySizeMaxColumn;

    @Size
    public List<Object> noSizeMinMaxColumn;

    @Size(min=-1)
    public List<Object> negativeSizeMinColumn;

    @Size(max=-2)
    public List<Object> negativeSizeMaxColumn;

    @Digits(integer=3,fraction=7)
    public float digitsColumn;
}
