package org.evomaster.core.mongo

class MongoInsertBuilder {

    fun createMongoInsertionAction(collection: String, documentsType: Class<*>, accessedFields: Map<String, Any>): List<MongoDbAction> {
        // FIX
        return mutableListOf(MongoDbAction(collection, documentsType, accessedFields))
    }
}
