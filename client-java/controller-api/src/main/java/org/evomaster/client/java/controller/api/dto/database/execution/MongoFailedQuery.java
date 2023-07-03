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
    private Class<?> documentsType;

    public MongoFailedQuery(String database, String collection, Class<?> documentsType) {
        this.database = database;
        this.collection = collection;
        this.documentsType = documentsType;
    }

    public MongoFailedQuery(){
        this.database = "";
        this.collection = "";
    }

    public String getDatabase() {return database;}
    public String getCollection() {return collection;}
    public Class<?> getDocumentsType() {
        return documentsType;
    }
}
