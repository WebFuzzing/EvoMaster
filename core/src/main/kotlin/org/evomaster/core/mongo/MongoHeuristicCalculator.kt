package org.evomaster.core.mongo

import org.bson.Document
import org.evomaster.core.mongo.filter.ASTNodeFilter
import org.evomaster.core.mongo.filter.FilterDistanceEvaluator

class MongoHeuristicCalculator {

    fun computeDistance(inputDocument: Document, filter: ASTNodeFilter): Double {
        val evaluator = FilterDistanceEvaluator()
        return filter.accept(evaluator, inputDocument)
    }

}