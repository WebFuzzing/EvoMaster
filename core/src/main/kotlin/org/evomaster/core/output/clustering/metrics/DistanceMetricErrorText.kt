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
 *  Note! Since only results with a 500 code have an error message, only those are considered for this
 *  clustering metric.
 *
 */

class DistanceMetricErrorText : DistanceMetric<RestCallResult>() {
    private val name = "ErrorText"
    override fun calculateDistance(first: RestCallResult, second: RestCallResult): Double {
        val message1 = if (includeInClustering(first)){
                /*first.getBodyType() != null
                && first.getStatusCode() == 500
                && (first.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && (first.getBody()?.trim()?.first()?.equals('[') == true || first.getBody()?.trim()?.first()?.equals('{') == true)) {

                 */
            Gson().fromJson(first.getBody(), Map::class.java)?.get("message") ?: ""
        }
        else {
            "" //first.getBody()
        }
        val message2 = if(includeInClustering(second)){

                /*second.getBodyType() != null
                && second.getStatusCode() == 500
                && (second.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && (second.getBody()?.trim()?.first()?.equals('[') == true || second.getBody()?.trim()?.first()?.equals('{') == true)) {

                 */
            Gson().fromJson(second.getBody(), Map::class.java)?.get("message") ?: ""
        }
        else {
            "" //second.getBody()
        }
        return LevenshteinDistance.distance(message1.toString(), message2.toString())
    }

    override fun getName(): String {
        return name
    }

    private fun includeInClustering(callResult: RestCallResult): Boolean{
        return callResult.getBodyType() != null
                && callResult.getStatusCode() == 500
                && (callResult.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && (callResult.getBody()?.trim()?.first()?.equals('[') == true || callResult.getBody()?.trim()?.first()?.equals('{') == true)
    }

}
