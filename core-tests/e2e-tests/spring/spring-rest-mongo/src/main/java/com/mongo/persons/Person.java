package com.mongo.persons;

import org.springframework.data.annotation.Id;

public class Person {
    @Id
    public String id;
    public Integer age;
    public Person(Integer age) {
        this.age = age;
    }
}

