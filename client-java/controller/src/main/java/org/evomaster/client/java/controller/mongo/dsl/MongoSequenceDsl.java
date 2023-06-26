package org.evomaster.client.java.controller.mongo.dsl;

public interface MongoSequenceDsl {

    /**
     * An insertion operation on the Mongo Database (MongoDB)
     *
     * @param databaseName the target database in the MongoDB
     * @param collectionName the target collection in the MongoDB
     * @return a statement in which it can be specified the values to add
     */
    MongoStatementDsl insertInto(String databaseName, String collectionName);
}
