package org.evomaster.client.java.instrumentation.example.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;

public interface MongoOperations {
    FindIterable<?> callFind(MongoCollection<?> mongoCollection, Bson bson);
}
