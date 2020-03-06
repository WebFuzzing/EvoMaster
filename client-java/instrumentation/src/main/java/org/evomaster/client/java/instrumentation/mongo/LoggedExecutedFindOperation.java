package org.evomaster.client.java.instrumentation.mongo;

import org.bson.Document;

public class LoggedExecutedFindOperation {

    private final String databaseName;

    private final String collectionName;

    private final Document queryDocument;

    private final boolean hasReturnedAnyDocument;

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
