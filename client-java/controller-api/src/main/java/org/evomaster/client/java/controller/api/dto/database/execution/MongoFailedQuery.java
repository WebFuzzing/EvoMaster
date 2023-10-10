package org.evomaster.client.java.controller.api.dto.database.execution;


public class MongoFailedQuery {
    /**
     * The database to insert the document into.
     */
    private final String database;
    /**
     * The collection to insert the document into.
     */
    private final String collection;
    /**
     * The type of the new document. Should map the type of the documents of the collection.
     */
    private String documentsType;

    public MongoFailedQuery(String database, String collection, String documentsType) {
        this.database = database;
        this.collection = collection;
        this.documentsType = documentsType;
    }

    public MongoFailedQuery(){
        this.database = "";
        this.collection = "";
        this.documentsType = "";
    }

    public String getDatabase() {return database;}
    public String getCollection() {return collection;}
    public String getDocumentsType() {
        return documentsType;
    }
}
