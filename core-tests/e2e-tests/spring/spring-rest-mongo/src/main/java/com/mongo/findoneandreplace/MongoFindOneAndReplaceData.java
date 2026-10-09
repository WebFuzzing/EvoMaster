package com.mongo.findoneandreplace;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.springframework.data.annotation.Id;

public class MongoFindOneAndReplaceData {

    /*
        The documents in the collection have an ObjectId as _id, while this is a String.
        As findOneAndReplace decodes the returned document with the driver's POJO codec,
        it needs to be told how to read it.
     */
    @Id
    @BsonRepresentation(BsonType.OBJECT_ID)
    public String id;

    public String city;

    public int age;

    public boolean verified;

    public MongoFindOneAndReplaceData() {
    }

    public MongoFindOneAndReplaceData(String city, int age, boolean verified) {
        this.city = city;
        this.age = age;
        this.verified = verified;
    }
}
