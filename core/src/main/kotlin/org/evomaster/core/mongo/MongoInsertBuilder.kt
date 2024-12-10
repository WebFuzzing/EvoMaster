package org.evomaster.core.mongo

class MongoInsertBuilder {
    fun createMongoInsertionAction(database: String, collection: String, documentsType: String): MongoDbAction{
        return MongoDbAction(database, collection, documentsType)
    }
}
