package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;

public class RPCTestDto {

    /**
     * name of the test case
     */
    public String testName;

    /**
     * a list of rpc actions in the test
     */
    public List<RPCActionDto> rpcFuctions;

    /**
     * a list of schedule task in the test
     */
    public List<ScheduleTaskInvocationDto> scheduleTaskInvocationDtos;

}
