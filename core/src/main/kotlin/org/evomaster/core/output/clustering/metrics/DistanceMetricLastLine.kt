package org.evomaster.core.output.clustering.metrics

import org.evomaster.core.problem.httpws.HttpWsCallResult

/**
 * Distance metric implementation for clustering Rest Call Results.
 *
 * The distance measurement is based on the Levenshtein distance, normalized by length.
 * Longer term, a means of weighting the clustering (to cluster first based on the module of the last executed line,
 * then class, then line, maybe?) can be attempted.
 *
 * The idea is to use the path to the last executed line in the SUT for clustering.
 *
 */

class DistanceMetricLastLine(
        epsilon: Double = -1.0
) : DistanceMetric<HttpWsCallResult>() {
    private val name = "LastLine"
    private var recommendedEpsilon = 0.0001
    private var usedEpsilon = if(epsilon in 0.0..1.0)  epsilon
                                        else recommendedEpsilon
                                            //throw IllegalArgumentException("The value of usedEpsilon is $epsilon. It should be between 0.0 and 1.0.")
    override fun calculateDistance(first: HttpWsCallResult, second: HttpWsCallResult): Double {
        val lastLine1 = first.getLastStatementWhen500() ?: ""
        val lastLine2 = second.getLastStatementWhen500() ?: ""
        //val l1 = LevenshteinDistance.distance(lastLine1, lastLine2)
        val l1 = FaultOriginDistance.distance(lastLine1, lastLine2)
        return l1
    }

    override fun getRecommendedEpsilon(): Double{
        return usedEpsilon
    }

    override fun getName(): String {
        return name
    }
    fun setusedEpsilon(epsilon: Double){
        if(epsilon in 0.0..1.0) usedEpsilon = epsilon
        else throw IllegalArgumentException("The value of usedEpsilon is $epsilon. It should be between 0.0 and 1.0.")
    }
}