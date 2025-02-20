package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.List;

/**
 * this dto is used to save info on a sequence of invoked schedule tasks
 */
public class ScheduleTaskInvocationsResult {

    public List<ScheduleTaskInvocationResultDto> results = new ArrayList<>();

    public String error500Msg;
}
