package org.evomaster.client.java.controller.mongo.dsl;

public interface MongoSequenceDsl {

    /**
     * An insertion operation on the Mongo Database (MongoDB)
     *
     * @param collectionName the target table in the DB
     * @return a statement in which it can be specified the values to add
     */
    MongoStatementDsl insertInto(String collectionName);
}
