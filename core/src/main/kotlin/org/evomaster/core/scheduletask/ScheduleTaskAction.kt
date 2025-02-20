package org.evomaster.core.scheduletask

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.Gene

/**
 * this action is designed to handle schedule task in Web APIs
 *
 *
 */
class ScheduleTaskAction(
    val taskId: String,
    val taskName: String,
    parameters: MutableList<Param>,
    /*
        extra info which is only needed for execution
        with customized techniques
        such info is not mutable
     */
    val immutableExtraInfo: Map<String, String?>? = null
): EnvironmentAction(parameters) {

    companion object{
        fun getScheduleTaskActionId(
            taskType: String,
            taskName: String
        ) = "$taskType:$taskName"

    }


    val parameters : List<Param>
        get() { return children as List<Param>}

    override fun getName(): String {
        return taskId
    }

    override fun seeTopGenes(): List<out Gene> {
        return parameters.flatMap { it.seeGenes() }
    }


    override fun copyContent(): ScheduleTaskAction {
        val p = parameters.asSequence().map(Param::copy).toMutableList()
        return ScheduleTaskAction(taskId, taskName, p, immutableExtraInfo?.toMap())
    }
}