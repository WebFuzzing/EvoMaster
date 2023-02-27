package com.foo.somedifferentpackage.examples.entity;


import javax.persistence.Entity;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Entity
public class EntityA {

    @Min(value = -1)
    public int minValueColumn;

    @Max(value = 200)
    public int maxValueColumn;

}
