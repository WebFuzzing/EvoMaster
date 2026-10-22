package com.foo.rpc.examples.spring.scheduled;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCTestDto;

import java.util.Collections;
import java.util.List;

public class OrderFulfillmentNoSeedController extends OrderFulfillmentController {

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {
        return Collections.emptyList();
    }
}
