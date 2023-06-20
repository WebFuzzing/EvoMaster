package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info about the type of documents in the collection.
 */
public class MongoCollectionInfo implements Serializable {
    private final String collectionName;
    private final Class<?> documentsType;

    public MongoCollectionInfo(String collectionName, Class<?> collectionType) {
        this.collectionName = collectionName;
        this.documentsType = collectionType;
    }

    public String getCollectionName() {return collectionName;}

    public Class<?> getDocumentsType() {return documentsType;}
}
