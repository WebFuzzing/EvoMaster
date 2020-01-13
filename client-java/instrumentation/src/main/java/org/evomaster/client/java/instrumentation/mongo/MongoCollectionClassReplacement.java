package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;

public class MongoCollectionClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return
                MongoCollection.class;
    }

    public static FindIterable<?> find(MongoCollection mongoCollection, Bson bson) {
        FindIterable<?> rv = mongoCollection.find(bson);

        BsonDocument query = bson.toBsonDocument(BsonDocument.class, mongoCollection.getCodecRegistry());
        boolean queryHasDocuments = mongoCollection.find(bson).iterator().hasNext();


        return rv;
    }

    public static FindIterable<?> find(MongoCollection mongoCollection, Bson bson, Class<?> documentClass) {
        return mongoCollection.find(bson, documentClass);
    }

    public static FindIterable<?> find(MongoCollection mongoCollection, ClientSession clientSession, Bson bson, Class<?> documentClass) {
        return mongoCollection.find(clientSession, bson, documentClass);
    }
}
