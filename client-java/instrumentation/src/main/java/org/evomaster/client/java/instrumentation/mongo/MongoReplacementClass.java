package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;

public class MongoReplacementClass implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return
                MongoCollection.class;
    }

    public static FindIterable<?> find(MongoCollection mongoCollection, Bson bson, Class<?> documentClass) {
        FindIterable<?> rv = mongoCollection.find(bson, documentClass);
        MongoLogger.getInstance().logFind(mongoCollection, bson);
        return rv;
    }

    public static FindIterable<?> find(MongoCollection mongoCollection, ClientSession clientSession, Bson bson, Class<?> documentClass) {
        FindIterable<?> rv = mongoCollection.find(clientSession, bson, documentClass);
        MongoLogger.getInstance().logFind(mongoCollection, bson);
        return rv;
    }
}
