package org.evomaster.core.mongo

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.QueryOperation
import org.evomaster.client.java.controller.mongo.selectors.*

class QueryParser {

    fun parse(query: Document): QueryOperation {
        val selectors = sequenceOf(
            EqualsSelector(),
            NotEqualsSelector(),
            LessThanEqualsSelector(),
            LessThanSelector(),
            GreaterThanSelector(),
            GreaterThanEqualsSelector(),
            AndSelector(),
            OrSelector(),
            InSelector(),
            NotInSelector(),
            AllSelector(),
            SizeSelector(),
            ModSelector(),
            NotSelector(),
            ExistsSelector(),
            TypeSelector(),
            ImplicitEqualsSelector()
        )
        return selectors.firstNotNullOf { it.getOperation(query) }
    }
}