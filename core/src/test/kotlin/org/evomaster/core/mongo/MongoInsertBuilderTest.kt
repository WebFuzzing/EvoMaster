package org.evomaster.core.mongo

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class MongoInsertBuilderTest {
    @Test
    fun testInsert() {
        val database = "aDatabase"
        val collection = "aCollection"
        val documentsType ="\"CustomType\":{\"CustomType\":{\"type\":\"object\", \"properties\": {\"aField\":{\"type\":\"string\"}}}}"
        val builder = MongoInsertBuilder()
        val action = builder.createMongoInsertionAction(database, collection, documentsType)

        assertEquals(database, action.database)
        assertEquals(collection, action.collection)
        assertEquals(documentsType, action.documentsType)
    }
}