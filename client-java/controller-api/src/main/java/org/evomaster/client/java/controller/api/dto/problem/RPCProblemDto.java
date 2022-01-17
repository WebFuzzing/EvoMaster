package org.evomaster.client.java.controller.api.dto.problem;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCInterfaceSchemaDto;

import java.util.List;

/**
 * a dto to collect info of RPC problem to be tested
 * that is sent to core
 */
public class RPCProblemDto {
    /**
     * a list of accessible endpoints grouped by interface
     */
    public List<RPCInterfaceSchemaDto> schemas;

}
