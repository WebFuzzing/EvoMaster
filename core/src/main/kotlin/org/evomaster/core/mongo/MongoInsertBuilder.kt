package org.evomaster.core.mongo

class MongoInsertBuilder {

    fun createMongoInsertionAction(database: String, collection: String, documentsType: Class<*>, accessedFields: Map<String, Class<*>>): List<MongoDbAction> {
        return mutableListOf(MongoDbAction(database, collection, documentsType, accessedFields))
    }
}
