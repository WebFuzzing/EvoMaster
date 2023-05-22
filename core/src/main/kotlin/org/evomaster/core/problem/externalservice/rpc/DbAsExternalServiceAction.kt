package org.evomaster.core.problem.externalservice.rpc

import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.parm.ClassResponseParam
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

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

        fun getDbAsExternalServiceAction(exAction: DbAsExternalServiceAction) = "DbAsExternalServiceAction${EXACTION_NAME_SEPARATOR}${exAction.commandName}${EXACTION_NAME_SEPARATOR}${exAction.requestRuleIdentifier?:"ANY"}$EXACTION_NAME_SEPARATOR${(exAction.response as ClassResponseParam).className}"
    }

    override fun getName(): String {
        return getDbAsExternalServiceAction(this)
    }

    override fun seeTopGenes(): List<out Gene> {
        return response.genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
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


}
