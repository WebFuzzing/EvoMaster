package org.evomaster.client.java.controller.api.dto.database.operations;

public class MongoInsertionDto {
    /**
     * The database to insert the document into.
     */
    public String databaseName;
    /**
     * The collection to insert the document into.
     */
    public String collectionName;
    /**
     * The type of the new document. Should map the type of the documents of the collection.
     */
    public String data;
}
