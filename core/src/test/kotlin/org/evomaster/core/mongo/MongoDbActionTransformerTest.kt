package org.evomaster.core.mongo

import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MongoDbActionTransformerTest {

    @Test
    fun testEmpty() {
        val actions = listOf<MongoDbAction>()
        val dto = MongoDbActionTransformer.transform(actions)
        assertTrue(dto.insertions.isEmpty())
    }

    @Test
    fun testNotEmpty() {
        val database = "aDatabase"
        val collection = "aCollection"
        val documentsType = "\"CustomType\":{\"CustomType\":{\"type\":\"object\", \"properties\": {\"aField\":{\"type\":\"string\"}}}}"
        val action = MongoDbAction(database, collection, documentsType)
        val actions = listOf(action)
        val dto = MongoDbActionTransformer.transform(actions)
        assertFalse(dto.insertions.isEmpty())
        assertTrue(dto.insertions[0].databaseName == action.database)
        assertTrue(dto.insertions[0].collectionName == action.collection)
        assertTrue(
            dto.insertions[0].data == action.seeTopGenes().first()
                .getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
        )
    }

}