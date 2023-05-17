package org.evomaster.client.java.controller.mongo;
public class MongoOperation {
    private final Object query;
    private final Object collection;

    public MongoOperation(Object collection, Object query) {
        this.collection = collection;
        this.query = query;
    }

    public Object getCollection() {
        return collection;
    }

    public Object getQuery() {
        return query;
    }
}
