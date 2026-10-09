package com.mongo.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Each endpoint has its own document type and collection, so that a document that
 * makes one endpoint return 200 cannot accidentally do it for another one.
 */
@Document(collection = "countDerivedCollection")
public class CountDerivedData {

    @Id
    public String id;

    public String city;

    public int age;

    public CountDerivedData() {
    }

    public CountDerivedData(String city, int age) {
        this.city = city;
        this.age = age;
    }
}
