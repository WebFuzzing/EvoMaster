package org.evomaster.core.problem.externalservice.rpc

import org.evomaster.client.java.controller.api.dto.MockDatabaseDto
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.parm.ClassResponseParam
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

/**
 * this is to handle database actions when it is handled with mock object
 */
class DbAsExternalServiceAction (
    /**
     * command name which will result in sql execution
     */
    val commandName : String,
    /**
     * descriptive info for the external service if needed
     * eg, app key for identifying the external service
     */
    val descriptiveInfo : String? = null,

    /**
     * accepted request
     * such rule is not mutable, and it can be specified only with seeded tests
     */
    val requestRuleIdentifier: String?,

    responseParam: ClassResponseParam,
    active : Boolean = false,
    used : Boolean = false
) : ApiExternalServiceAction(responseParam, active, used) {

    companion object{
        fun getDbAsExternalServiceAction(
            dbDto : MockDatabaseDto
        ) =  getDbAsExternalServiceAction(dbDto.commandName, dbDto.requests, dbDto.responseFullTypeWithGeneric?:dbDto.responseFullType)



        fun getDbAsExternalServiceAction(
            commandName: String,
            requestRuleIdentifier: String?,
            responseClassName: String
        ) = "DbAsExternalServiceAction${EXACTION_NAME_SEPARATOR}${commandName}${EXACTION_NAME_SEPARATOR}${requestRuleIdentifier?:"ANY"}$EXACTION_NAME_SEPARATOR${responseClassName}"
    }

    override fun getName(): String {
        return getDbAsExternalServiceAction(commandName, requestRuleIdentifier, (response as ClassResponseParam).className)
    }

    override fun seeTopGenes(): List<out Gene> {
        return response.genes
    }


    override fun copyContent(): StructuralElement {
        return DbAsExternalServiceAction(commandName, descriptiveInfo, requestRuleIdentifier, response.copy() as ClassResponseParam, active, used)
    }

    /**
     * @return a copy of DbAsExternalServiceAction and release its restricted request identifier
     */
    fun getUnrestrictedDbAsExternalServiceAction(): DbAsExternalServiceAction {
        return DbAsExternalServiceAction(commandName, descriptiveInfo, null, response.copy() as ClassResponseParam)
    }

    fun addUpdateForParam(param: Param){
        addChild(param)
    }
}
