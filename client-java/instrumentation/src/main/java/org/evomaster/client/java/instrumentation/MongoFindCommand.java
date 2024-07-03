package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info related to MONGO command execution.
 */
public class MongoFindCommand implements Serializable {
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
     * Executed FIND query
     */
    private final Object bson;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public MongoFindCommand(String databaseName, String collectionName, String documentsType, Object bson, boolean successfullyExecuted, long executionTime) {
        this.collectionName = collectionName;
        this.databaseName = databaseName;
        this.documentsType = documentsType;
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

    public String getDocumentsType() {return documentsType;}

    public String getDatabaseName() {
        return databaseName;
    }
}
