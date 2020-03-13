package org.evomaster.core.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.conversions.Bson
import org.evomaster.core.mongo.filter.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocumentToFilterConverterTest {

    companion object {
        private fun toDocument(filter: Bson): Document {
            val bsonDocument = filter.toBsonDocument(BsonDocument::class.java, MongoClient.getDefaultCodecRegistry())!!
            return DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build())
        }
    }

    @Test
    fun convertEquals() {
        val filterBson = Filters.eq("age", 0)
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("age", filter.fieldName)
        assertEquals(0, filter.value)
    }

    @Test
    fun convertGreaterThan() {
        val filterBson = Filters.gt("age", 0)
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val query = converter.translate(filterDocument)
        assertTrue(query is ComparisonFilter<*>)
        query as ComparisonFilter<Integer>
        assertEquals("age", query.fieldName)
        assertEquals(0, query.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.GREATER_THAN, query.operator)
    }

    @Test
    fun convertGreaterEqualsThan() {
        val filterBson = Filters.gte("age", 0)
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("age", filter.fieldName)
        assertEquals(0, filter.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.GREATER_THAN_EQUALS, filter.operator)

    }

    @Test
    fun convertLessThan() {
        val filterBson = Filters.lt("age", 0)
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("age", filter.fieldName)
        assertEquals(0, filter.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.LESS_THAN, filter.operator)
    }

    @Test
    fun convertLessThanEquals() {
        val filterBson = Filters.lte("age", 0)
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val query = converter.translate(filterDocument)
        assertTrue(query is ComparisonFilter<*>)
        query as ComparisonFilter<Integer>
        assertEquals("age", query.fieldName)
        assertEquals(0, query.value)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.LESS_THAN_EQUALS, query.operator)
    }

    @Test
    fun convertStringEquals() {
        val filterBson = Filters.eq("name", "John")
        val filterDocument = toDocument(filterBson)
        val parser = DocumentToASTFilterConverter()
        val filter = parser.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("name", filter.fieldName)
        assertEquals("John", filter.value)
    }

    @Test
    fun convertAnd() {
        val filterBson = Filters.and(
                Filters.lte("age", 0),
                Filters.eq("name", "John"))
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val query = converter.translate(filterDocument)
        assertTrue(query is AndFilter)
        query as AndFilter

        val left = query.filters[0]
        val right = query.filters[0]

        assertTrue(left is ComparisonFilter<*>)
        assertTrue(right is ComparisonFilter<*>)
    }

    @Test
    fun convertOr() {
        val filterBson = Filters.or(
                Filters.lte("age", 0),
                Filters.eq("name", "John"))
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is OrFilter)
        filter as OrFilter

        val left = filter.filters[0]
        val right = filter.filters[0]

        assertTrue(left is ComparisonFilter<*>)
        assertTrue(right is ComparisonFilter<*>)
    }

    @Test
    fun convertAll() {
        val allFilter = Filters.all("colors", "Blue", "Red", "White"
        )
        val filterDocument = toDocument(allFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is AllFilter)
        filter as AllFilter

        val fieldName = filter.fieldName
        val values = filter.values

        assertEquals("colors", fieldName)
        assertEquals(listOf("Blue", "Red", "White"), values)
    }

    @Test
    fun convertElemMatch() {
        val elemMatchFilter = Filters.elemMatch("statusLog", Filters.eq("status", "Submitted"))
        val filterDocument = toDocument(elemMatchFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ElemMatchFilter)
        filter as ElemMatchFilter

        val elemMatchFieldName = filter.fieldName
        assertEquals("statusLog", elemMatchFieldName)

        val elemMatchValue = filter.filter
        assertTrue(elemMatchValue is ComparisonFilter<*>)
        elemMatchValue as ComparisonFilter<*>
        assertEquals("status", elemMatchValue.fieldName)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.EQUALS, elemMatchValue.operator)
        assertEquals("Submitted", elemMatchValue.value)
    }

    @Test
    fun convertSize() {
        val elemMatchFilter = Filters.size("entries", 10)
        val filterDocument = toDocument(elemMatchFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is SizeFilter)
        filter as SizeFilter

        assertEquals("entries", filter.fieldName)
        assertEquals(10, filter.size)
    }

    @Test
    fun convertNotEquals() {
        val filterBson = Filters.ne("name", "John")
        val filterDocument = toDocument(filterBson)
        val parser = DocumentToASTFilterConverter()
        val filter = parser.translate(filterDocument)
        assertTrue(filter is ComparisonFilter<*>)
        filter as ComparisonFilter<Integer>
        assertEquals("name", filter.fieldName)
        assertEquals(ComparisonFilter.ComparisonQueryOperator.NOT_EQUALS, filter.operator)
        assertEquals("John", filter.value)
    }

    @Test
    fun convertIn() {
        val allFilter = Filters.`in`("colors", "Blue", "White", "Red")
        val filterDocument = toDocument(allFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is InFilter)
        filter as InFilter

        val fieldName = filter.fieldName
        val values = filter.values

        assertEquals("colors", fieldName)
        assertEquals(listOf("Blue", "White", "Red"), values)
    }

    @Test
    fun convertNotIn() {
        val allFilter = Filters.nin("colors", "Blue", "White", "Red")
        val filterDocument = toDocument(allFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is NotInFilter)
        filter as NotInFilter

        val fieldName = filter.fieldName
        val values = filter.values

        assertEquals("colors", fieldName)
        assertEquals(listOf("Blue", "White", "Red"), values)
    }

    @Test
    fun convertNor() {
        val filterBson = Filters.nor(
                Filters.lte("age", 0),
                Filters.eq("name", "John"))
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is NorFilter)
        filter as NorFilter

        val left = filter.filters[0]
        val right = filter.filters[0]

        assertTrue(left is ComparisonFilter<*>)
        assertTrue(right is ComparisonFilter<*>)
    }

    @Test
    fun convertExists() {
        val filterBson = Filters.exists("age")
        val filterDocument = toDocument(filterBson)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is ExistsFilter)
        filter as ExistsFilter

        assertEquals("age", filter.fieldName)
    }

    @Test
    fun convertRegex() {
        val elemMatchFilter = Filters.regex("email", ".*,*", "")
        val filterDocument = toDocument(elemMatchFilter)
        val converter = DocumentToASTFilterConverter()
        val filter = converter.translate(filterDocument)
        assertTrue(filter is RegexFilter)
        filter as RegexFilter

        assertEquals("email", filter.fieldName)
        assertEquals(".*,*", filter.pattern)
        assertEquals("", filter.options)

    }
}