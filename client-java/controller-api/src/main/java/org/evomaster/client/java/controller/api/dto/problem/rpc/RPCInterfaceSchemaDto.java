package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.List;

/**
 * a dto to collect info of an RPC interface to be tested
 * that is sent to core
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

    /**
     * a list of endpoints in this interface which are used for handling auth of the SUT
     * note that the auth could be used by other endpoints, eg, login
     */
    public List<RPCActionDto> authEndpoints;

    /**
     * a list of reference of the endpoints, and
     * the reference is identified based on the index where to be specified in the driver
     * note that the auth might be only applicable for some specific endpoints,
     * therefore, we define a unique reference for each authEndpoint
     */
    public List<Integer> authEndpointReferences;

    /**
     * a list of identified types of responses from the given client
     */
    public List<ParamDto> identifiedResponseTypes;

}
