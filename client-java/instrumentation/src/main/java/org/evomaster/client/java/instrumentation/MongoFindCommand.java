package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.mongo.BsonDocumentConverter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.evomaster.client.java.instrumentation.mongo.BsonDocumentConverter.isBsonDocument;

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


    private final /*org.bson.BsonDocument*/ Object bsonDocument;

    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    /**
     * Constructor for MongoFindCommand.
     *
     * @param databaseName
     * @param collectionName
     * @param documentsType
     * @param bsonDocument an instance of org.bson.BsonDocument
     * @param successfullyExecuted
     * @param executionTime
     */
    public MongoFindCommand(String databaseName, String collectionName, String documentsType, /*org.bson.BsonDocument*/ Object bsonDocument, boolean successfullyExecuted, long executionTime) {
        if (bsonDocument!=null && !isBsonDocument(bsonDocument)) {
            throw new IllegalArgumentException("bsonDocument must be an instance of org.bson.BsonDocument but it is of class " + bsonDocument.getClass().getName());
        }
        this.collectionName = collectionName;
        this.databaseName = databaseName;
        this.documentsType = documentsType;
        this.bsonDocument = bsonDocument;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public Object getQuery() {
        return bsonDocument;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getDocumentsType() {
        return documentsType;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Indicates whether the find command
     * was successfully executed or it
     * threw a RuntimeException due to
     * an invalid query.
     *
     * @return if the lazy command was successfully executed
     */
    public boolean isSuccessfullyExecuted() {
        return successfullyExecuted;
    }




}
