package org.evomaster.client.java.controller.api.dto.problem.rpc;

import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.EndpointSchema;

/**
 * created by manzhang on 2021/11/12
 */
public class RPCActionDto extends ActionDto {

    /**
     * id of interface of this RPC request
     */
    public String interfaceId;

    /**
     * a RPC request
     */
    public EndpointSchema rpcCall;
}
