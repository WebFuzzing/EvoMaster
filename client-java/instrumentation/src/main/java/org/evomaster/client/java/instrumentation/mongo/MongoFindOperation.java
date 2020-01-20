package org.evomaster.client.java.instrumentation.mongo;

import org.bson.Document;

public class MongoFindOperation extends MongoOperation {

    public MongoFindOperation(String databaseName, String collectionName, Document query) {
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.query = query;
    }

    private final String databaseName;

    private final String collectionName;

    private final Document query;

    public Document getQuery() {
        return query;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

}
