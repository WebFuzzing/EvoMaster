package org.evomaster.core.problem.rest.data

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.IdHeuristics
import org.evomaster.core.problem.rest.IdLocationValue
import org.evomaster.core.search.action.Action
import javax.ws.rs.core.MediaType


class RestCallResult : HttpWsCallResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    @VisibleForTesting
    internal constructor(other: RestCallResult) : super(other)

    companion object {
        const val HEURISTICS_FOR_CHAINED_LOCATION = "HEURISTICS_FOR_CHAINED_LOCATION"
    }


    override fun copy(): RestCallResult {
        return RestCallResult(this)
    }


    fun getResourceId(): IdLocationValue? {

        /*
            TODO should use more sophisticated algorithm, taking into account what done in RestResponseFeeder
         */

        if(!MediaType.APPLICATION_JSON_TYPE.isCompatible(getBodyType())){
            //TODO could also handle other media types
            return null
        }

        val body = getBody() ?: return null
        return IdHeuristics.getId(body)
    }


    /**
     * It might happen that we need to chain call location, but
     * there was no "location" header in the response of call that
     * created a new resource. However, it "might" still be possible
     * to try to infer the location (based on object response and
     * the other available endpoints in the API)
     */
    fun setHeuristicsForChainedLocation(on: Boolean) = addResultValue(HEURISTICS_FOR_CHAINED_LOCATION, on.toString())
    fun getHeuristicsForChainedLocation(): Boolean = getResultValue(HEURISTICS_FOR_CHAINED_LOCATION)?.toBoolean() ?: false

    override fun matchedType(action: Action): Boolean {
        return action is RestCallAction
    }
}
