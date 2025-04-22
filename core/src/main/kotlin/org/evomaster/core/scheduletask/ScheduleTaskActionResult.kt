package org.evomaster.core.scheduletask

import com.google.common.annotations.VisibleForTesting
import org.evomaster.client.java.controller.api.dto.problem.rpc.ExecutionStatusDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationResultDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsResult
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.search.action.ActionResult

class ScheduleTaskActionResult : ActionResult {

    companion object {
        const val EXECUTION_STATUS_VARIABLE_NAME = "SCHEDULE_TASK_EXECUTION_STATUS"
        const val EXECUTION_STATUS_RUNNING = "RUNNING"
        const val EXECUTION_STATUS_COMPLETED = "COMPLETED"
        const val EXECUTION_STATUS_FAILED = "FAILED"
    }


    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    internal constructor(other: ScheduleTaskActionResult): super(other)

    override fun copy(): ScheduleTaskActionResult {
        return ScheduleTaskActionResult(this)
    }

    fun getExecutionStatus() = getResultValue(EXECUTION_STATUS_VARIABLE_NAME)

    fun taskCompleted(){
        addResultValue(EXECUTION_STATUS_VARIABLE_NAME, EXECUTION_STATUS_COMPLETED)
    }

    fun taskStartedAndRunning(){
        addResultValue(EXECUTION_STATUS_VARIABLE_NAME, EXECUTION_STATUS_RUNNING)
    }

    fun taskStartedButFailed(){
        addResultValue(EXECUTION_STATUS_VARIABLE_NAME, EXECUTION_STATUS_FAILED)
    }

    fun setResultBasedOnDto(dto: ScheduleTaskInvocationResultDto){
        when(dto.status){
            ExecutionStatusDto.FAILED -> taskStartedButFailed()
            ExecutionStatusDto.RUNNING -> taskStartedAndRunning()
            ExecutionStatusDto.COMPLETED -> taskCompleted()
        }
        if (dto.status == ExecutionStatusDto.FAILED && dto.errorMsg != null){
            setErrorMessage(dto.errorMsg!!)
        }
    }
}