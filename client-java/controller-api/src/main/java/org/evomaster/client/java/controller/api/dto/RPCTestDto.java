package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.rpc.EvaluatedRPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.MockRPCExternalServiceDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;

import java.util.List;

/**
 * a dto represent RPC tests
 */
public class RPCTestDto {

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
    public List<EvaluatedRPCActionDto> actions;

    /**
     * a list of response correspoding to the actions
     */
    public List<ActionResponseDto> responses;
}
