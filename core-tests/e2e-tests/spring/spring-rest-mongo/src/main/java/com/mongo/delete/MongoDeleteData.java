package com.mongo.delete;

import org.springframework.data.annotation.Id;

public class MongoDeleteData {

    @Id
    public String id;

    public String city;

    public int age;

    public MongoDeleteData() {
    }

    public MongoDeleteData(String city, int age) {
        this.city = city;
        this.age = age;
    }
}
