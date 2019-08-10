package com.foo.graphql.examples.spring.petshop.entities;

import lombok.Data;
import com.foo.graphql.examples.spring.petshop.enums.Animal;

@Data
public class Pet {
    private long id;

    private String name;

    private Animal type;

    private int age;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    public void setType(Animal type) {
        this.type = type;
    }

    public Animal getType() {
        return type;
    }
}