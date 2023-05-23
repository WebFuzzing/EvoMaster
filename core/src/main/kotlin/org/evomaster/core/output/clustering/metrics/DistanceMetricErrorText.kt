package org.evomaster.core.output.clustering.metrics

import org.evomaster.core.problem.httpws.HttpWsCallResult
import javax.ws.rs.core.MediaType


/**
 *  Distance metric implementation for clustering strings.
 *
 *  The actual distance used is based on Levenshtein Distance, normalized by string length.
 *
 *  The intended use is to cluster error messages coming from REST APIs to enable similar
 *  faults to be grouped together for easier debugging/analysis.
 *
 *  Note! Since only results with a 500 code have an error message, only those are considered for this
 *  clustering metric.
 *
 */

class DistanceMetricErrorText(
        epsilon: Double = -1.0
) : DistanceMetric<HttpWsCallResult>() {
    private val name = "ErrorText"
    private val recommendedEpsilon = 0.001
    private val usedEpsilon = if (epsilon in 0.0..1.0) epsilon
                                    else recommendedEpsilon
                                        //throw IllegalArgumentException("The value of usedEpsilon is $epsilon. It should be between 0.0 and 1.0.")
    override fun calculateDistance(first: HttpWsCallResult, second: HttpWsCallResult): Double {
        val message1 = if (includeInClustering(first)){
            first.getErrorMsg() ?: first.getBody().toString()
        } else {
            //"" //first.getBody()
            first.getBody().toString()
        }
        val message2 = if(includeInClustering(second)){
            second.getErrorMsg() ?: first.getBody().toString()
        } else {
            //"" //second.getBody()
            second.getBody().toString()
        }
        val l1 = LevenshteinDistance.distance(message1, message2)
        return l1
    }

    override fun getRecommendedEpsilon(): Double {
        return usedEpsilon
    }

    override fun getName(): String {
        return name
    }


    private fun includeInClustering(callResult: HttpWsCallResult): Boolean{
        return callResult.getBodyType() != null
                && callResult.getStatusCode() == 500
                && (callResult.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && (callResult.getBody()?.trim()?.first()?.equals('[') == true || callResult.getBody()?.trim()?.first()?.equals('{') == true)
    }

}
