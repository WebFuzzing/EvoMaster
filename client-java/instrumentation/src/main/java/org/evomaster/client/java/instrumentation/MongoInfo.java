package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info related to MONGO command execution.
 */
public class MongoInfo implements Serializable {
    private final Object collection;
    private final Object bson;
    private final boolean successfullyExecuted;
    private final long executionTime;

    public MongoInfo(Object collection, Object bson, boolean successfullyExecuted, long executionTime) {
        this.collection = collection;
        this.bson = bson;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public Object getQuery() {
        return bson;
    }
    public Object getCollection() {
        return collection;
    }
}
