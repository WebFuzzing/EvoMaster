package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/27
 */
public class RPCActionDto {

    /**
     * name of the RPC interface
     */
    public String interfaceId;

    /**
     * name of the client
     */
    public String clientInfo;

    /**
     * name of the action
     */
    public String actionName;

    /**
     * request params
     */
    public List<ParamDto> requestParams;

    /**
     * response param (nullable)
     */
    public ParamDto responseParam;

    /**
     * variable name of response
     */
    public String responseVariable;

    /**
     * variable name of controller
     */
    public String controllerVariable;

    /**
     * if generate assertions on driver side and send back core
     */
    public boolean doGenerateAssertions;

    /**
     * if generate test script on driver side and send back core
     */
    public boolean doGenerateTestScript;

    /**
     * if the action requires auth to access
     */
    public boolean isAuthorized;

    /**
     * possible candidates for auth to access the endpoint
     */
    public List<Integer> requiredAuthCandidates;

    public RPCActionDto copy(){
        RPCActionDto copy = new RPCActionDto();
        copy.interfaceId = interfaceId;
        copy.clientInfo = clientInfo;
        copy.actionName = actionName;
        copy.responseParam = responseParam;
        if (requestParams != null)
            copy.requestParams = requestParams.stream().map(ParamDto::copy).collect(Collectors.toList());
        copy.responseVariable = responseVariable;
        copy.controllerVariable = controllerVariable;
        copy.doGenerateAssertions = doGenerateAssertions;
        copy.doGenerateTestScript = doGenerateTestScript;
        copy.isAuthorized = isAuthorized;
        if (copy.requiredAuthCandidates != null)
            copy.requiredAuthCandidates = new ArrayList<>(requiredAuthCandidates);
        return copy;
    }

}
