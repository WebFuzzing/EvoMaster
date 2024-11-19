package org.evomaster.core.problem.scheduletask

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.Gene

/**
 * this action is designed to handle schedule task in Web APIs
 */
class ScheduleTaskAction(
    val taskId: String,
    parameters: MutableList<Param>,
): EnvironmentAction(parameters) {

    companion object{
        fun getScheduleTaskActionId(
            taskType: String,
            taskName: String
        ) = "ScheduleTaskAction"

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
        return ScheduleTaskAction(taskId, p)
    }
}