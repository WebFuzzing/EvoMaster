package org.evomaster.core.problem.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.evomaster.client.java.controller.api.dto.database.execution.epa.Enabled
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import javax.ws.rs.core.MediaType


class RestCallResult : HttpWsCallResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    companion object {
        const val HEURISTICS_FOR_CHAINED_LOCATION = "HEURISTICS_FOR_CHAINED_LOCATION"
        const val INITIAL_ENABLED_ENDPOINTS = "INITIAL_ENABLED_ENDPOINTS"
        const val ENABLED_ENDPOINTS_AFTER_ACTION = "ENABLED_ENDPOINTS_AFTER_ACTION"
        val mapper = jacksonObjectMapper()
    }


    override fun copy(): ActionResult {
        return RestCallResult(this)
    }

    fun getResourceIdName() = "id"

    fun getResourceId(): String? {

        if(!MediaType.APPLICATION_JSON_TYPE.isCompatible(getBodyType())){
            //TODO could also handle other media types
            return null
        }

        return getBody()?.let {
            try {
                /*
                    TODO: "id" is the most common word, but could check
                    if others are used as well.
                 */
                Gson().fromJson(it, JsonObject::class.java).get(getResourceIdName())?.asString
            } catch (e: Exception){
                //nothing to do
                null
            }
        }
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
    fun setInitialEnabledEndpoints(enabledActions: RestActions?) = addResultValue(INITIAL_ENABLED_ENDPOINTS, mapper.writeValueAsString(enabledActions))
    fun getInitialEnabledEndpoints(): RestActions? {
        return getResultValue(INITIAL_ENABLED_ENDPOINTS)?.let { mapper.readValue<RestActions?>(it) }
    }
    fun setEnabledEndpointsAfterAction(enabledActions: Enabled) = addResultValue(ENABLED_ENDPOINTS_AFTER_ACTION, mapper.writeValueAsString(enabledActions))
    fun getEnabledEndpointsAfterAction(): Enabled? {
        return getResultValue(ENABLED_ENDPOINTS_AFTER_ACTION)?.let { mapper.readValue<Enabled?>(it) }
    }

    override fun matchedType(action: Action): Boolean {
        return action is RestCallAction
    }
}
