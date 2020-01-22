package org.evomaster.core.output.clustering.metrics

import com.google.gson.Gson
import org.evomaster.core.problem.rest.RestCallResult
import javax.ws.rs.core.MediaType
import kotlin.math.max


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
    override fun calculateDistance(val1: RestCallResult, val2: RestCallResult): Double {
        val message1 = if (val1.getBodyType() != null
                && (val1.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            Gson().fromJson(val1.getBody(), Map::class.java)?.get("message") ?: ""
        }
        else {
            val1.getBody()
        }
        val message2 = if(val2.getBodyType() != null
                && (val2.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            Gson().fromJson(val2.getBody(), Map::class.java)?.get("message") ?: ""
        }
        else {
            val2.getBody()
        }
        return LevenshteinDistance.distance(message1.toString(), message2.toString())
    }
}
