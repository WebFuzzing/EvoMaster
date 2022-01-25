package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.InterfaceSchema;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * created by manzhang on 2021/11/12
 */
public abstract class RPCEndpointsBuilderTestBase {

    class FakeClient{}

    public InterfaceSchema schema = RPCEndpointsBuilder.build(getInterfaceName(), getRPCType(), new FakeClient(), null, null, null, null, getAuthInfo(), getCustomizedValueInRequests());

    public abstract String getInterfaceName();

    public abstract int expectedNumberOfEndpoints();

    public List<CustomizedRequestValueDto> getCustomizedValueInRequests(){
        return null;
    }

    public List<AuthenticationDto> getAuthInfo(){return null;}

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

    public boolean containType(List<NamedTypedValue> params, String fullTypeName){
        return params.stream().anyMatch(s-> s.getType().getFullTypeName().equals(fullTypeName));
    }
}
