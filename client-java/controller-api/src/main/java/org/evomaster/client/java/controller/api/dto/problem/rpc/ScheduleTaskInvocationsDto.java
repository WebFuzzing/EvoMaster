package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.List;


/**
 * to reduce times of making http calls, all schedule tasks in a test will be handled one time.
 */
public class ScheduleTaskInvocationsDto {

    public List<ScheduleTaskInvocationDto> tasks = new ArrayList<>();
}
