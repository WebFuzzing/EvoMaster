package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;

public class ScheduleTaskInvocationDto {


    /**
     * name for the schedule task
     */
    private String taskName;

    /**
     * request params (nullable)
     */
    public List<ParamDto> requestParams;

    /**
     * response param (nullable)
     */
    public ParamDto responseParam;


    /**
     * the type of schedule task which used in customization
     */
    public String scheduleTaskType;

    /**
     * the descriptive info of schedule task which used in customization
     */
    public String descriptiveInfo;

    /**
     * the host name or ip where the task can be found which used in customization
     */
    public String hostName;
}
