package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.InterfaceSchema;
import org.evomaster.client.java.controller.problem.RPCType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * created by manzhang on 2021/11/12
 */
public abstract class RPCEndpointsBuilderTestBase {

    public InterfaceSchema schema = RPCEndpointsBuilder.build(getInterfaceName(), getRPCType());

    public abstract String getInterfaceName();

    public abstract int expectedNumberOfEndpoints();

    public RPCType getRPCType(){
        return RPCType.THRIFT;
    }

    public EndpointSchema getOneEndpoint(String name){
        List<EndpointSchema> endpoints = schema.findEndpoints(name);
        assertEquals(1, endpoints.size());
        return endpoints.get(0);
    }

    public void getNullEndpoint(String name){
        List<EndpointSchema> endpoints = schema.findEndpoints(name);
        assertEquals(0, endpoints.size());
    }

    public List<EndpointSchema> getListEndpoint(String name, int expectedSize){
        List<EndpointSchema> endpoints = schema.findEndpoints(name);
        assertEquals(expectedSize, endpoints.size());
        return endpoints;
    }
}
