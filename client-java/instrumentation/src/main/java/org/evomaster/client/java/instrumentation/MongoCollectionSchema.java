package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of documents in a collection.
 */
public class MongoCollectionSchema implements Serializable {
    private final String collectionName;
    private final String collectionSchema;

    public MongoCollectionSchema(String collectionName, String collectionSchema) {
        this.collectionName = collectionName;
        this.collectionSchema = collectionSchema;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getCollectionSchema() {
        return collectionSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoCollectionSchema that = (MongoCollectionSchema) o;
        return Objects.equals(collectionName, that.collectionName) && Objects.equals(collectionSchema, that.collectionSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionName, collectionSchema);
    }

    @Override
    public String toString() {
        return "MongoCollectionSchema{" +
                "collectionName='" + collectionName + '\'' +
                ", collectionSchema='" + collectionSchema + '\'' +
                '}';
    }
}
