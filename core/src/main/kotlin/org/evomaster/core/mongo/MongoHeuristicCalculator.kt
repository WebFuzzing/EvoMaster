package org.evomaster.core.mongo

import org.bson.Document
import org.evomaster.core.mongo.filter.ASTNodeFilter
import org.evomaster.core.mongo.filter.DistanceEvaluator

class MongoHeuristicCalculator {

    fun computeDistance(inputDocument: Document, filter: ASTNodeFilter): Double {
        val evaluator = DistanceEvaluator()
        return filter.accept(evaluator, inputDocument)
    }

}