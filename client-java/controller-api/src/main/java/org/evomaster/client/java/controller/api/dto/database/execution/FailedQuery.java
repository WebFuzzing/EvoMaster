package org.evomaster.client.java.controller.api.dto.database.execution;


public class FailedQuery {
    public FailedQuery(String database, String collection, Class<?> documentsType) {
        this.database = database;
        this.collection = collection;
        this.documentsType = documentsType;
    }

    public FailedQuery(){
        this.database = "";
        this.collection = "";
    }

    private final String database;
    private final String collection;
    private Class<?> documentsType;

    public String getDatabase() {return database;}
    public String getCollection() {return collection;}
    public Class<?> getDocumentsType() {
        return documentsType;
    }
}
