package org.evomaster.core.output.clustering.metrics

import com.google.gson.Gson
import org.evomaster.core.problem.rest.RestCallResult
import javax.ws.rs.core.MediaType


/**
 *  Distance metric implementation for clustering strings.
 *
 *  The actual distance used is based on Levenshtein Distance, normalized by string length.
 *
 *  The intended use is to cluster error messages coming from REST APIs to enable similar
 *  faults to be grouped together for easier debugging/analysis.
 *
 */

class DistanceMetricAction : DistanceMetric<RestCallResult>() {
    override fun calculateDistance(first: RestCallResult, second: RestCallResult): Double {
        val message1 = if (first.getBodyType() != null
                && (first.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            Gson().fromJson(first.getBody(), Map::class.java)?.get("message") ?: ""
        }
        else {
            first.getBody()
        }
        val message2 = if(second.getBodyType() != null
                && (second.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            Gson().fromJson(second.getBody(), Map::class.java)?.get("message") ?: ""
        }
        else {
            second.getBody()
        }
        return LevenshteinDistance.distance(message1.toString(), message2.toString())
    }
}
