package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;

/**
 * created by manzhang on 2021/11/27
 */
public class RPCInterfaceSchemaDto {

    /**
     * name of the interface
     */
    public String interfaceId;

    /**
     * name of client which could execute endpoints of this interface
     */
    public String clientInfo;

    /**
     * endpoints in this RPC interface
     */
    public List<RPCActionDto> endpoints;

    /**
     * defined types in this interface
     */
    public List<ParamDto> types;

    /**
     *  skipped endpoints
     */
    public List<String> skippedEndpoints;

    public List<RPCActionDto> authEndpoints;

    public List<Integer> authEndpointReferences;

}
