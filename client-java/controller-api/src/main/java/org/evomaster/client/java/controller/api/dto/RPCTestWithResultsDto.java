package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionWithResultDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.MockRPCExternalServiceDto;

import java.util.List;

/**
 * a dto represent RPC tests with executed results
 */
public class RPCTestWithResultsDto {

    /**
     * a list of mocking handling for the tests
     */
    public List<MockRPCExternalServiceDto> externalServiceDtos;

    /**
     * a list of sql handling for the tests
     */
    public List<String> sqlInsertions;

    /**
     * a list of RPC actions
     */
    public List<RPCActionWithResultDto> actions;

    /**
     * a list of response correspoding to the actions
     */
    public List<ActionResponseDto> responses;
}
