package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * this class is to save info after the schedule task is invoked
 * the info is used to track the status (eg, completed, running, failed) of the schedule task
 *
 * TODO
 * As the schedule task can also be applied to other problem, eg, REST, GraphQL
 * in the future, need to refactor this class to other package
 */
public class ScheduleTaskInvocationResultDto {

    /**
     *  invocation id
     */
    public String invocationId;

    /**
     *  schedule task name
     */
    public String taskName;

    /**
     * app key for the API if needed
     */
    public String appKey;

    /**
     * status after the schedule is invoked
     */
    public ExecutionStatusDto status = ExecutionStatusDto.RUNNING;

    /**
     * it is used to save any error message if have
     */
    public String errorMsg;
}
