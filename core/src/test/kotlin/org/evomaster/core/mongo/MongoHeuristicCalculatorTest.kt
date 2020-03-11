package org.evomaster.core.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.junit.jupiter.api.Test

class MongoHeuristicCalculatorTest {

    private fun toDocument(filter: Bson): Document {
        val bsonDocument = filter.toBsonDocument(BsonDocument::class.java, MongoClient.getDefaultCodecRegistry())!!
        return DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
    }

    @Test
    fun testEquality() {
        val document = Document()
        document.append("age", 32)

        val filterBson = Filters.eq("age", 0)
        val filterDocument = toDocument(filterBson)

        val distance = MongoHeuristicCalculator().computeDistance(document, filterDocument)

    }
}