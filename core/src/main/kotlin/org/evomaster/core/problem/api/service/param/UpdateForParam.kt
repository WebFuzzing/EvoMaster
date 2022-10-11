package org.evomaster.core.problem.api.service.param

interface UpdateForParam{

    /**
     * @return if the [param] is updated as [this]
     */
    fun isUpdatedParam(param: Param) : Boolean
}