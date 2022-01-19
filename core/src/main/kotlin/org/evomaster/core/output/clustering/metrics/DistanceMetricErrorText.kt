package org.evomaster.core.output.clustering.metrics

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
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
        epsilon: Double = 0.6
) : DistanceMetric<HttpWsCallResult>() {
    private val name = "ErrorText"
    private val recommendedEpsilon = if (epsilon in 0.0..1.0) epsilon
                                    else throw IllegalArgumentException("The value of recommendedEpsilon is $epsilon. It should be between 0.0 and 1.0.")
    override fun calculateDistance(first: HttpWsCallResult, second: HttpWsCallResult): Double {
        val message1 = if (includeInClustering(first)){
            getMessage(first.getBody())
        }
        else {
            "" //first.getBody()
        }
        val message2 = if(includeInClustering(second)){
            getMessage(second.getBody())
        }
        else {
            "" //second.getBody()
        }
        return LevenshteinDistance.distance(message1, message2)
    }

    override fun getRecommendedEpsilon(): Double {
        return recommendedEpsilon
    }

    override fun getName(): String {
        return name
    }

    private fun getMessage(body: String?) : String{
        if(body == null){
            return ""
        }

        return try{
            Gson().fromJson(body, Map::class.java)?.get("message").toString() ?: ""
        }catch (e: JsonSyntaxException){
            ""
        }
    }

    private fun includeInClustering(callResult: HttpWsCallResult): Boolean{
        return callResult.getBodyType() != null
                && callResult.getStatusCode() == 500
                && (callResult.getBodyType() as MediaType).isCompatible(MediaType.APPLICATION_JSON_TYPE)
                && (callResult.getBody()?.trim()?.first()?.equals('[') == true || callResult.getBody()?.trim()?.first()?.equals('{') == true)
    }

}
