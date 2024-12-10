package com.foo.somedifferentpackage.examples.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity(name="BAR")
public class EntityY {

    @NotNull
    public Boolean x;

    @Column(name = "foo")
    public int y;

    @NotNull
    @Column(name="hello")
    private Double z;

    private long k;
}
