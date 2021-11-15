package org.evomaster.client.java.controller.api.dto.problem;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.InterfaceSchema;

import java.util.List;

/**
 * created by manzhang on 2021/11/4
 */
public class RPCProblemDto {
    /**
     * a list of accessible endpoints grouped by interface
     */
    public List<InterfaceSchema> schemas;
}
