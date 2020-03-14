package org.evomaster.core.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.core.mongo.filter.ASTNodeFilter
import org.evomaster.core.mongo.filter.DocumentToASTFilterConverter
import org.junit.jupiter.api.Test

class MongoHeuristicCalculatorTest {

    companion object {
        private fun toFilter(filter: Bson): ASTNodeFilter {
            val bsonDocument = filter.toBsonDocument(BsonDocument::class.java, MongoClient.getDefaultCodecRegistry())!!
            val document = DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
            return DocumentToASTFilterConverter().translate(document)
        }
    }

    @Test
    fun testEquality() {
        val document = Document()
        document.append("age", 32)

        val filterBson = Filters.eq("age", 0)
        val filterDocument = toFilter(filterBson)

        val distance = MongoHeuristicCalculator().computeDistance(document, filterDocument)

    }
}