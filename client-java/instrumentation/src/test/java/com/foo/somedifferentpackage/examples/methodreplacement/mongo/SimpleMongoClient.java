package com.foo.somedifferentpackage.examples.methodreplacement.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;

public class SimpleMongoClient {

    public FindIterable<?> invokeFind(MongoCollection<?> mongoCollection, Bson bson, Class<?> documentClass) {
        return mongoCollection.find(bson, documentClass);
    }

    public FindIterable<?> invokeFind(MongoCollection<?> mongoCollection, ClientSession clientSession, Bson bson, Class<?> documentClass) {
        return mongoCollection.find(clientSession, bson, documentClass);
    }

}
