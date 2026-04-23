package com.mongo.students;

import org.springframework.data.annotation.Id;

public class Student {

    @Id
    public String id;
    public String firstName;
    public String lastName;

    public Student(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

