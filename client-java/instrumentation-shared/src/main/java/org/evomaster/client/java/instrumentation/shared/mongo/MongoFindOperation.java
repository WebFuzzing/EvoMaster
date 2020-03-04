package org.evomaster.client.java.instrumentation.shared.mongo;

import org.bson.Document;

public class MongoFindOperation extends MongoOperation {

    public MongoFindOperation(String databaseName, String collectionName, Document query, boolean hasOperationFoundAnyDocuments) {
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.query = query;
        this.hasOperationFoundAnyDocuments = hasOperationFoundAnyDocuments;
    }

    private final String databaseName;

    private final String collectionName;

    private final Document query;

    private final boolean hasOperationFoundAnyDocuments;

    public Document getQuery() {
        return query;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public boolean hasOperationFoundAnyDocuments() {
        return hasOperationFoundAnyDocuments;
    }

}
