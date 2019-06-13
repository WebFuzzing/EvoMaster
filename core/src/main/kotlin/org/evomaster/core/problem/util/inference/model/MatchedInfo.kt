package org.evomaster.core.problem.rest.util.inference.model

import org.evomaster.core.problem.rest.util.ParserUtil
import kotlin.math.min

open class MatchedInfo(val input : String, val targetMatched : String, var similarity : Double, var inputIndicator : Int = 0, var outputIndicator : Int = 0){

    fun modifySimilarity(times : Double = 0.9){
        similarity *= times
        if (similarity > 1.0) similarity = 1.0
    }

    fun setMax(){
        similarity = 1.0
    }

    fun setMin(){
        similarity = min(similarity, ParserUtil.SimilarityThreshold)
    }
}
