package org.evomaster.client.java.instrumentation.mongo;

import org.bson.Document;

public class LoggedExecutedFindOperation {

    private String databaseName;

    private String collectionName;

    private Document queryDocument;

    private boolean hasReturnedAnyDocument;

    public LoggedExecutedFindOperation() {

    }

    public LoggedExecutedFindOperation(String databaseName, String collectionName, Document queryDocument, boolean hasReturnedAnyDocument) {
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.queryDocument = queryDocument;
        this.hasReturnedAnyDocument = hasReturnedAnyDocument;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public Document getQueryDocument() {
        return queryDocument;
    }

    public boolean hasReturnedAnyDocument() {
        return hasReturnedAnyDocument;
    }

}
