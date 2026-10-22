package com.mongo.update;

import org.springframework.data.annotation.Id;

public class MongoUpdateData {

    @Id
    public String id;

    public String city;

    public int age;

    public boolean verified;

    public MongoUpdateData() {
    }

    public MongoUpdateData(String city, int age, boolean verified) {
        this.city = city;
        this.age = age;
        this.verified = verified;
    }
}
