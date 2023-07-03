package org.evomaster.client.java.controller.mongo;

public class MongoOperation {
    /**
     * Should be an implementation of class {@link MongoOperation#queryClass}
     */
    private final Object query;
    /**
     * Should be an implementation of class {@link MongoOperation#collectionClass}
     */
    private final Object collection;

    private final String collectionClass = "com.mongodb.client.MongoCollection";

    private final String queryClass = "org.bson.conversions.Bson";

    public MongoOperation(Object collection, Object query) {
        if (!isImplementationOfMongoCollection(collection))
            throw new java.lang.IllegalArgumentException("collection must be of type " + collectionClass);
        if (!isImplementationOfBson(query))
            throw new java.lang.IllegalArgumentException("query must be of type " + queryClass);
        this.collection = collection;
        this.query = query;
    }

    public Object getCollection() {
        return collection;
    }

    public Object getQuery() {
        return query;
    }

    private boolean isImplementationOfMongoCollection(Object collection) {
        return implementsInterface(collection, collectionClass);
    }

    private boolean isImplementationOfBson(Object query) {
        return implementsInterface(query, queryClass);
    }

    private boolean implementsInterface(Object obj, String interfaceName) {
        Class<?>[] interfaces = obj.getClass().getInterfaces();
        for (Class<?> intf : interfaces) {
            if (intf.getName().equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }
}
