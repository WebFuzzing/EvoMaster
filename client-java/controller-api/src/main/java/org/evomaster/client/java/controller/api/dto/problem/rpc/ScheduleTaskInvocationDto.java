package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;

public class ScheduleTaskInvocationDto {


    /**
     * The index of this schedule action in the test.
     */
    public Integer index = null;

    /**
     * name for the schedule task
     */
    public String taskName;

    /**
     * request params (nullable)
     */
    public List<ParamDto> requestParams;


    /**
     * request params without knowing types
     */
    public List<String> requestParamsAsStrings;

    /**
     * response param (nullable)
     */
    public ParamDto responseParam;


    /**
     * id referring to Web API under test
     */
    public String appKey;

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
