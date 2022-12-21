package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.EqualsOperation
import org.evomaster.client.java.controller.mongo.operations.NotOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation
import org.evomaster.core.mongo.QueryParser

class NotSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) {
            return null
        }

        val fieldName = query.keys.first()

        val child = query[fieldName]
        if (child !is Document) {
            return null
        }

        val notOperator = child.keys.first()

        if (notOperator != "\$not") {
            return null
        }

        val innerDocument = child[notOperator]

        if (innerDocument !is Document) {
            return null
        }

        // This is necessary for query parser to work correctly as the syntax for not is different
        // The field is at beginning instead
        val docWithRemovedNot = Document().append(fieldName, innerDocument)

        val filter = QueryParser().parse(docWithRemovedNot)

        return NotOperation(fieldName, filter)
    }
}