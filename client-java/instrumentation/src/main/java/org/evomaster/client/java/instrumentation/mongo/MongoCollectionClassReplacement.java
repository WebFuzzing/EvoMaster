package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.MongoCollection;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;

public class MongoCollectionClassReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return
                MongoCollection.class;
    }
/*
    @Replacement(type = ReplacementType.EXCEPTION)
    public FindIterable<?> find(MongoCollection mongoCollection, String idTemplate) {
        return mongoCollection.find();
    }

    @Replacement(type = ReplacementType.EXCEPTION)
    public long countDocuments(MongoCollection mongoCollection, String idTemplate) {
        return mongoCollection.countDocuments();
    }
    */
}
