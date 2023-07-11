package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info about the type of documents in the collection.
 */
public class MongoCollectionInfo implements Serializable {
    private final String collectionName;
    private final String documentsType;

    public MongoCollectionInfo(String collectionName, String documentsType) {
        this.collectionName = collectionName;
        this.documentsType = documentsType;
    }

    public String getCollectionName() {return collectionName;}

    public String getDocumentsType() {return documentsType;}
}
