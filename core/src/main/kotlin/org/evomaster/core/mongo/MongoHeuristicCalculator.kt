package org.evomaster.core.mongo

import org.bson.Document
import org.evomaster.core.mongo.filter.DistanceEvaluator
import org.evomaster.core.mongo.filter.DocumentToASTFilterConverter

class MongoHeuristicCalculator {

    fun computeDistance(inputDocument: Document, filterDocument: Document): Double {
        val parser = DocumentToASTFilterConverter()
        val filterAst = parser.translate(filterDocument)
        val evaluator = DistanceEvaluator()
        return filterAst.accept(evaluator, inputDocument)
    }


}