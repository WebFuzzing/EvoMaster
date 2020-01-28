package org.evomaster.core.output.clustering.metrics

import org.evomaster.core.problem.rest.RestCallResult

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

class DistanceMetricLastLine : DistanceMetric<RestCallResult>() {
    override fun calculateDistance(val1: RestCallResult, val2: RestCallResult): Double {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        val lastLine1 = val1.getLastStatementWhen500() ?: ""
        val lastLine2 = val2.getLastStatementWhen500() ?: ""
        return LevenshteinDistance.distance(lastLine1, lastLine2)
    }
}