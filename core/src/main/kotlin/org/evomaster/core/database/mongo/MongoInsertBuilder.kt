package org.evomaster.core.database.mongo

class MongoInsertBuilder {
    fun createMongoInsertionAction(database: String, collection: String, documentsType: String): MongoDbAction{
        return MongoDbAction(database, collection, documentsType).apply { forceNewTaints() }
    }
}
