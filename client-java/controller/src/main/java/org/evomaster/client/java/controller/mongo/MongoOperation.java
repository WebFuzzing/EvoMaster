package org.evomaster.client.java.controller.mongo;

public class MongoOperation {
    /**
     * Executed FIND query
     * Should be an implementation of class {@link MongoOperation#queryClass}
     */
    private final Object query;

    /**
     * Name of the collection that the operation was applied to
     */
    private final String collectionName;

    /**
     * Name of the database that the operation was applied to
     */
    private final String databaseName;

    /**
     * Type of the documents of the collection
     */
    private final String documentsType;

    private final String queryClass = "org.bson.conversions.Bson";

    public MongoOperation(String collectionName, Object query, String databaseName, String documentsType) {
        if (!isImplementationOfBson(query)) {
            throw new IllegalArgumentException("query must be of type " + queryClass);
        }
        this.collectionName = collectionName;
        this.databaseName = databaseName;
        this.documentsType = documentsType;
        this.query = query;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public Object getQuery() {
        return query;
    }

    public String getDocumentsType() {return documentsType;}

    private boolean isImplementationOfBson(Object obj) {
        Class<?>[] interfaces = obj.getClass().getInterfaces();
        for (Class<?> intf : interfaces) {
            if (intf.getName().equals(queryClass)) return true;
        }
        return false;
    }
}
