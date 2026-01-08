package org.evomaster.core.scheduletask

import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsResult


/**
 * this interface is to handle invocations of schedule tasks
 */
interface ScheduleTaskExecutor {

    /**
     * Invoke a list of schedule tasks
     * Return true if it was successful.
     */
    fun invokeScheduleTasksAndGetResults(dtos: ScheduleTaskInvocationsDto): ScheduleTaskInvocationsResult?

}