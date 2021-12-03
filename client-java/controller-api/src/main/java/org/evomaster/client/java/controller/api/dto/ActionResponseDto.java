package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;

/**
 * handling response of actions which are executed in driver side
 */
public class ActionResponseDto {

    /**
     * The index of this action which was executed in this test.
     */
    public Integer index = null;


    /**
     * a response from an RPC action.
     * note that this info is only used for RPC problem
     */
    public ParamDto rpcResponse;
}
