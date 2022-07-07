package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;


/**
 * seeded RPC test
 */
public class SeededRPCTestDto {

    /**
     * a name of the test
     */
    public String testName;

    /**
     * a list of info to setup responses of external services with mocking if exists
     */
    public List<MockRPCExternalServiceDto> mockRPCExternalServiceDtos;

    /**
     * a list of rpc functions in the test
     */
    public List<SeededRPCActionDto> rpcFunctions;
}
