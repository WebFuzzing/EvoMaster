package com.foo.somedifferentpackage.examples.methodreplacement;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.example.mongo.MongoOperations;

public class MongoOperationsImpl implements MongoOperations {

    @Override
    public FindIterable<?> callFind(MongoCollection<?> mongoCollection, Bson bson){
        return mongoCollection.find(bson);
    }

}
