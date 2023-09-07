package org.evomaster.client.java.controller.api.dto.problem.rpc;

public class ExpandRPCInfoDto {

    /**
     * expanded schema
     */
    public RPCInterfaceSchemaDto schemaDto;

    /**
     * expanded action which comprise mock object
     * such as external API, databases
     */
    public RPCActionDto expandActionDto;
}
