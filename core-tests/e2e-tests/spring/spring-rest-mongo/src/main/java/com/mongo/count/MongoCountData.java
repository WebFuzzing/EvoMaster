package com.mongo.count;

import org.springframework.data.annotation.Id;

public class MongoCountData {

    @Id
    public String id;

    public String city;

    public int age;

    public MongoCountData() {
    }

    public MongoCountData(String city, int age) {
        this.city = city;
        this.age = age;
    }
}
