package com.mongo.template;

import org.springframework.data.annotation.Id;

public class MongoTemplateData {

    @Id
    public String id;

    public String data;

    public MongoTemplateData(String data) {
        this.data = data;
    }
}
