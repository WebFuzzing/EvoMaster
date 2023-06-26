package org.evomaster.core.mongo

class MongoInsertBuilder {
    fun createMongoInsertionAction(database: String, collection: String, documentsType: Class<*>): MongoDbAction{
        return MongoDbAction(database, collection, documentsType)
    }
}
