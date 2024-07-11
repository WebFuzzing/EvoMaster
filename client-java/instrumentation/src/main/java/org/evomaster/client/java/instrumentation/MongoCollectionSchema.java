package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Schema of documents in a collection.
 */
public class MongoCollectionSchema implements Serializable {
    private final String collectionName;
    private final String collectionSchema;

    public MongoCollectionSchema(String collectionName, String collectionSchema) {
        this.collectionName = collectionName;
        this.collectionSchema = collectionSchema;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getCollectionSchema() {
        return collectionSchema;
    }
}
