package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;

/**
 * DTO defines necessary info to invoke or terminate the schedule task
 */
public class CustomizedScheduleTaskInvocationDto {

    /**
     * id referring to Web API under test
     */
    public String appKey;


    /**
     * a name referring to schedule task
     */
    public String taskName;

    /**
     * a list of value of input parameters if the task requires
     * nullable if the input parameters are not needed
     */
    public List<String> inputParameterValues;


    /**
     * the type of schedule task
     */
    public String scheduleTaskType;

    /**
     * the descriptive info of schedule task
     */
    public String descriptiveInfo;

    /**
     * the host name or ip where the task can be found
     */
    public String hostName;

}
