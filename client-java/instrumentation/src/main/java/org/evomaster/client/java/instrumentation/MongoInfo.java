package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info related to MONGO command execution.
 */
public class MongoInfo implements Serializable {
    /**
     * Name of the collection that the operation was applied to
     */
    private final String collectionName;

    /**
     * Name of the database that the operation was applied to
     */
    private final String databaseName;

    /**
     * Type of the documents of the collection
     */
    private final String documentsType;

    /**
     * Documents in the collection at the moment of the operation
     */
    private final Iterable<?> documents;

    /**
     * Executed FIND query
     */
    private final Object bson;
    private final boolean successfullyExecuted;
    private final long executionTime;

    public MongoInfo(String collectionName, String databaseName, String documentsType, Iterable<?> documents, Object bson, boolean successfullyExecuted, long executionTime) {
        this.collectionName = collectionName;
        this.databaseName = databaseName;
        this.documentsType = documentsType;
        this.documents = documents;
        this.bson = bson;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public Object getQuery() {
        return bson;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public Iterable<?> getDocuments() {
        return documents;
    }

    public String getDocumentsType() {return documentsType;}

    public String getDatabaseName() {
        return databaseName;
    }
}
