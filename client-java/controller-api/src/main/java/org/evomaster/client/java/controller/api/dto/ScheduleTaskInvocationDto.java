package org.evomaster.client.java.controller.api.dto;

import java.util.List;

/**
 * DTO defines necessary info to invoke or terminate the schedule task
 */
public class ScheduleTaskInvocationDto {

    /**
     * id referring to Web API under test
     */
    public String appKey;


    /**
     * a name referring to schedule task
     */
    public String taskName;

    /**
     * a list of name of input parameters if the name is accessible
     * nullable if the names are not accessible or there is no input parameters
     */
    public List<String> inputParameterNames;
    /**
     * a list of value of input parameters if the task requires
     * nullable if the input parameters are not needed
     */
    public List<String> inputParameterValues;

    /**
     * a list of type according to input parameters
     * null if the inputParameterValues is null
     * otherwise the size should be same with the inputParameterValues
     */
    public List<String> inputParameterTypes;

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
