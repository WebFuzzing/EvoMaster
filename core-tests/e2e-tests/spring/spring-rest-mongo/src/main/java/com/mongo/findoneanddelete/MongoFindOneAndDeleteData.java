package com.mongo.findoneanddelete;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.springframework.data.annotation.Id;

public class MongoFindOneAndDeleteData {

    /*
        The documents in the collection have an ObjectId as _id, while this is a String.
        As findOneAndDelete decodes the deleted document with the driver's POJO codec,
        it needs to be told how to read it.
     */
    @Id
    @BsonRepresentation(BsonType.OBJECT_ID)
    public String id;

    public String city;

    public int age;

    public MongoFindOneAndDeleteData() {
    }

    public MongoFindOneAndDeleteData(String city, int age) {
        this.city = city;
        this.age = age;
    }
}
