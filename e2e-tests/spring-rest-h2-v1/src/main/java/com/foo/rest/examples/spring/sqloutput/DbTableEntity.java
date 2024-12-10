package com.foo.rest.examples.spring.sqloutput;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class DbTableEntity {

    @Id @GeneratedValue
    private Long id;

    private String name;

    public DbTableEntity(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
