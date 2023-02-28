package com.foo.somedifferentpackage.examples.entity;


import javax.persistence.Entity;
import javax.validation.constraints.*;

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

}
