package com.mongo.template;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;

public class MongoTemplateData {

    @Id
    public String id;

    @NotNull
    public String name;

    @NotNull
    public int age;

    @NotNull
    public String city;

    public MongoTemplateData() {
    }
}
