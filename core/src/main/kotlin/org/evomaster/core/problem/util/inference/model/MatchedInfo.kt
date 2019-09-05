package org.evomaster.core.problem.rest.util.inference.model

import org.evomaster.core.problem.util.StringSimilarityComparator
import kotlin.math.min


/**
 * the class is used when applying parser to derive possible relationship between two named entities (e.g., resource with table name)
 * @property input for matching
 * @property targetMatched presents what are matched regarding a target
 * @property similarity presents a degree of similarity
 * @property inputIndicator presents a depth-level of input, e.g., token on resource path is level 0, token on description is level 1
 * @property outputIndicator presents a depth-level of target to match, e.g., name of table is level 0, name of a column of a table is level 1
 */
open class MatchedInfo(val input : String, val targetMatched : String, var similarity : Double, var inputIndicator : Int = 0, var outputIndicator : Int = 0){

    fun modifySimilarity(times : Double = 0.9){
        similarity *= times
        if (similarity > 1.0) similarity = 1.0
    }

    fun setMax(){
        similarity = 1.0
    }

    fun setMin(){
        similarity = min(similarity, StringSimilarityComparator.SimilarityThreshold)
    }
}
