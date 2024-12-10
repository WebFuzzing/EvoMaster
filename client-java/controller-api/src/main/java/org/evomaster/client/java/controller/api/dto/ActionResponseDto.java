package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ExpandRPCInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCInterfaceSchemaDto;

import java.util.List;
import java.util.Map;

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
     *
     * in generated test scripts, the action could be involved by either
     * 1) executeRPCAction (a method in SutHandler) or
     * 2) client.endpoint(args).
     *
     * For 1), we could process it from core side,
     * while for 2), now it is handled from driver side.
     *
     * this attribute is to enable 2) generation.
     */
    public List<String> testScript;

    /**
     * code for assertions
     *
     * currently, the assertion generation is processed
     * on driver side based on response instance returned after
     * the endpoint is invoked.
     *
     * then this attribute contains a list of generated scripts
     * for assertions on the response if it is not `void`
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

    /**
     * with rpc, we need to return generated scripts and assertions
     * then instead of only return msg, we return this dto with error msg
     */
    public String error500Msg;

    /**
     * for RPC problem, its schema or mock object might be expanded during the search
     */
    public ExpandRPCInfoDto expandInfo;

}
