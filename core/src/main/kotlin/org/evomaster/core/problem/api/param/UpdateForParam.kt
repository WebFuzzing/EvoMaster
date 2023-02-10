package org.evomaster.core.problem.api.param

interface UpdateForParam{

    /**
     * @return if the [param] is updated as [this]
     */
    fun isSameTypeWithUpdatedParam(param: Param) : Boolean

    fun getUpdatedParam() : Param
}