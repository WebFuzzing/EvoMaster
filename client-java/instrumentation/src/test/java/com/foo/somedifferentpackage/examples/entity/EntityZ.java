package com.foo.somedifferentpackage.examples.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class EntityZ {

    @Enumerated(EnumType.STRING)
    public Foo foo;
}
