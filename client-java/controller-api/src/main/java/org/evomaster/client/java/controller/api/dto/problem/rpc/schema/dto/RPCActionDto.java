package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto;

import java.util.List;

/**
 * created by manzhang on 2021/11/27
 */
public class RPCActionDto {

    /**
     * name of the RPC interface
     */
    public String interfaceId;

    /**
     * name of the action
     */
    public String actionId;

    /**
     * request params
     */
    public List<ParamDto> requestParams;

    /**
     * response param (nullable)
     */
    public ParamDto responseParam;

}
