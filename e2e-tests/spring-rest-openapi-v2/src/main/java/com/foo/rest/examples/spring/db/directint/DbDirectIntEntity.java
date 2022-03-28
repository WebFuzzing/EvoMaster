package com.foo.rest.examples.spring.db.directint;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
public class DbDirectIntEntity {

    @Id @GeneratedValue
    private Long id;

    @NotNull
    private Integer x;

    @NotNull
    private Integer y;

    public DbDirectIntEntity(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}
