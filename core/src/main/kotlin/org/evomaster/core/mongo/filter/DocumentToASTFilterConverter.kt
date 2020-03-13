package org.evomaster.core.mongo.filter

import org.bson.BsonRegularExpression
import org.bson.Document

class DocumentToASTFilterConverter {

    companion object {
        private val EQUALS_OPERATOR = "\$eq"

        private val GREATER_THAN_OPERATOR = "\$gt"

        private val GREATER_THAN_EQUALS_OPERATOR = "\$gte"

        private val LESS_THAN_OPERATOR = "\$lt"

        private val LESS_THAN_EQUALS_OPERATOR = "\$lte"

        private val OR_OPERATOR = "\$or"

        private val ALL_OPERATOR = "\$all"

        private val ELEM_MATCH_OPERATOR = "\$elemMatch"

        private val SIZE_OPERATOR = "\$size"

        private val NOT_EQUALS_OPERATOR = "\$ne"

        private val IN_OPERATOR = "\$in"

        private val NOT_IN_OPERATOR = "\$nin"

        private val NOR_OPERATOR = "\$nor"

        private val EXISTS_OPERATOR = "\$exists"

    }

    fun translate(filterDocument: Document): ASTNodeFilter {

        var filter: ASTNodeFilter?

        filter = handleAll(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleIn(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleNotIn(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleSize(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleElemMatch(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleOr(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleNor(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleOperatorComparison(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleExists(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleRegex(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleSimpleEquality(filterDocument)
        if (filter != null) {
            return filter
        }

        filter = handleSimpleAnd(filterDocument)
        if (filter != null) {
            return filter
        }

        throw IllegalArgumentException("Unsupported filter document $filterDocument")
    }

    private fun handleElemMatch(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            return null
        }

        val fieldName = document.keys.first()

        val elemMatch = document[fieldName]

        if (elemMatch !is Document) {
            return null
        }

        if (!isUniqueEntry(elemMatch)) {
            return null
        }

        val elemMatchOperator = elemMatch.keys.first()

        if (elemMatchOperator != ELEM_MATCH_OPERATOR) {
            return null
        }

        val elemMatchValue = elemMatch[elemMatchOperator]
        if (elemMatchValue !is Document) {
            return null
        }

        val filter = translate(elemMatchValue)

        return ElemMatchFilter(fieldName, filter)

    }

    private fun handleAll(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            // root of all filters must have one entry
            return null
        }

        val keyName = document.keys.first()


        val child = document[keyName]

        if (child !is Document) {
            return null
        }

        if (!isUniqueEntry(child)) {
            return null
        }

        val firstKey = child.keys.first()

        if (firstKey != ALL_OPERATOR) {
            return null
        }

        val values = child[firstKey]

        if (values !is List<*>) {
            return null
        }

        return AllFilter(keyName, values)
    }


    private fun handleIn(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            // root of all filters must have one entry
            return null
        }

        val keyName = document.keys.first()


        val child = document[keyName]

        if (child !is Document) {
            return null
        }

        if (!isUniqueEntry(child)) {
            return null
        }

        val firstKey = child.keys.first()

        if (firstKey != IN_OPERATOR) {
            return null
        }

        val values = child[firstKey]

        if (values !is List<*>) {
            return null
        }

        return InFilter(keyName, values)
    }

    private fun handleNotIn(document: Document): ASTNodeFilter? {

        if (!isUniqueEntry(document)) {
            // root of all filters must have one entry
            return null
        }

        val keyName = document.keys.first()


        val child = document[keyName]

        if (child !is Document) {
            return null
        }

        if (!isUniqueEntry(child)) {
            return null
        }

        val firstKey = child.keys.first()

        if (firstKey != NOT_IN_OPERATOR) {
            return null
        }

        val values = child[firstKey]

        if (values !is List<*>) {
            return null
        }

        return NotInFilter(keyName, values)
    }

    private fun handleSize(document: Document): ASTNodeFilter? {

        if (!isUniqueEntry(document)) {
            // root of all filters must have one entry
            return null
        }

        val keyName = document.keys.first()


        val child = document[keyName]

        if (child !is Document) {
            return null
        }

        if (!isUniqueEntry(child)) {
            return null
        }

        val sizeOperator = child.keys.first()

        if (sizeOperator != SIZE_OPERATOR) {
            return null
        }

        val size = child[sizeOperator]

        if (size !is Int) {
            return null
        }

        return SizeFilter(keyName, size)
    }

    private fun handleSimpleAnd(filter: Document): ASTNodeFilter? {
        if (isUniqueEntry(filter)) {
            // and operations must have at least two children
            return null
        }

        val queries = mutableListOf<ASTNodeFilter>()
        filter.entries.forEach {
            val doc = Document()
            doc.put(it.key, it.value)
            val query = translate(doc)
            queries.add(query)
        }
        return AndFilter(queries.toList())
    }

    private fun handleOr(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            // or operations must have one entry
            return null
        }

        val keyName = document.keys.first()
        if (keyName != OR_OPERATOR) {
            return null
        }

        val child = document[keyName]

        val queries = mutableListOf<ASTNodeFilter>()
        if (child is List<*>) {
            child.forEach {
                it as Document
                queries.add(translate(it))
            }
        }

        return OrFilter(queries.toList())
    }

    private fun handleNor(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            // or operations must have one entry
            return null
        }

        val keyName = document.keys.first()
        if (keyName != NOR_OPERATOR) {
            return null
        }

        val child = document[keyName]

        val queries = mutableListOf<ASTNodeFilter>()
        if (child is List<*>) {
            child.forEach {
                it as Document
                queries.add(translate(it))
            }
        }

        return NorFilter(queries.toList())
    }


    private fun isUniqueEntry(map: Map<*, *>) = map.size == 1


    private fun handleExists(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            return null
        }

        val fieldName = document.keys.first()
        val value = document[fieldName]

        if (value !is Document) {
            return null
        }

        if (!isUniqueEntry(value)) {
            return null
        }

        val existsOperator = value.keys.first()
        if (existsOperator != EXISTS_OPERATOR) {
            return null
        }

        val existsPolarity = value[existsOperator]

        if (existsPolarity !is Boolean) {
            return null
        }

        if (existsPolarity) {
            return ExistsFilter(fieldName)
        } else {
            return NotExistsFilter(fieldName)
        }
    }

    private fun handleRegex(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            return null
        }

        val fieldName = document.keys.first()
        val value = document[fieldName]

        if (value !is BsonRegularExpression) {
            return null
        }

        return RegexFilter(fieldName, value.pattern, value.options)
    }

    private fun handleSimpleEquality(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            return null
        }

        val fieldName = document.keys.first()
        val value = document[fieldName]

        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.EQUALS, value)
    }

    private fun handleOperatorComparison(document: Document): ASTNodeFilter? {
        if (!isUniqueEntry(document)) {
            return null
        }

        val fieldName = document.keys.first()
        val value = document[fieldName]

        if (value is Document) {
            if (isUniqueEntry(value)) {
                val operator = value.keys.first()
                val comparisonValue = value[operator]
                when (operator) {
                    EQUALS_OPERATOR ->
                        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.EQUALS, comparisonValue)
                    GREATER_THAN_OPERATOR ->
                        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.GREATER_THAN, comparisonValue)
                    GREATER_THAN_EQUALS_OPERATOR ->
                        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.GREATER_THAN_EQUALS, comparisonValue)
                    LESS_THAN_EQUALS_OPERATOR ->
                        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.LESS_THAN_EQUALS, comparisonValue)
                    LESS_THAN_OPERATOR ->
                        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.LESS_THAN, comparisonValue)
                    NOT_EQUALS_OPERATOR ->
                        return ComparisonFilter(fieldName, ComparisonFilter.ComparisonQueryOperator.NOT_EQUALS, comparisonValue)

                }
            }
        }

        return null
    }

}