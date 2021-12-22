package org.evomaster.client.java.controller.api.dto.problem.rpc;

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

    public RPCActionDto copy(){
        RPCActionDto copy = new RPCActionDto();
        copy.interfaceId = interfaceId;
        copy.clientInfo = clientInfo;
        copy.actionName = actionName;
        copy.responseParam = responseParam;
        if (requestParams != null)
            copy.requestParams = requestParams.stream().map(ParamDto::copy).collect(Collectors.toList());
        copy.responseVariable = responseVariable;
        return copy;
    }

}
