package org.evomaster.client.java.controller.mongo

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.QueryOperation
import org.evomaster.client.java.controller.mongo.selectors.*

/**
 * Determines to which operation a query correspond.
 */
class QueryParser {

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
        ElemMatchSelector(),
        ModSelector(),
        NotSelector(),
        ExistsSelector(),
        TypeSelector(),
        ImplicitEqualsSelector()
    )
    fun parse(query: Document): QueryOperation { return selectors.firstNotNullOf { it.getOperation(query) } }
}