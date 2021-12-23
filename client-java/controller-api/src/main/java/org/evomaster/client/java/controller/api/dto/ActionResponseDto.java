package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto;

import java.util.List;

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

    /**
     *  an original response with json format
     */
    public String jsonResponse;

    /**
     * code for processing the action
     */
    public List<String> testScript;

    /**
     * code for assertions
     */
    public List<String> assertionScript;

    /**
     * representing exception info if it occurs
     */
    public RPCExceptionInfoDto exceptionInfoDto;


    /**
     * representing a result categorized by user's specification
     */
    public CustomizedCallResultCode customizedCallResultCode;


}
