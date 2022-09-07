package org.evomaster.core.problem.external.service

import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.Action

abstract class ApiExternalServiceAction(
    /**
     * response to return if the external service is accessed
     *
     * To Andrea,
     * a type of external service might contain a certain type of response
     * eg, HTTP response
     * Do we need to make the response as a generic type T where T extends ResponseParam
     * and bind the ApiExternalServiceAction with the T?
     */
    val response: ResponseParam,
    active : Boolean = false,
    used : Boolean = false,
    localId : String
) : Action(listOf(response), localId){


    /**
     * it represents whether the external service is instanced before executing the corresponding API call
     *
     * Note that it should be always re-assigned based on [used] before fitness evaluation, see [resetActive]
     */
    var active : Boolean = active
        private set


    /**
     * it represents whether the external service is used when executing the corresponding API call
     *
     * Note that it should be updated after the fitness evaluation based on whether the external service is used during the API execution
     */
    var used : Boolean = used
        private set




    /**
     * reset active based on [used]
     * it should be used before fitness evaluation
     */
    fun resetActive() {
        this.active = this.used
    }

    /**
     * based on the feedback from the mocked service,
     * the external service is confirmed that it is used during the API execution
     */
    fun confirmUsed()  {
        this.used = true
    }

    /**
     * based on the feedback from the mocked service,
     * the external service is confirmed that it is not used during the API execution
     */
    fun confirmNotUsed() {
        this.used = false
    }
}