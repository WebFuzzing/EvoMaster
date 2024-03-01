package org.evomaster.client.java.controller.problem.rpc;

import com.thrift.example.artificial.Necessity;
import com.thrift.example.artificial.RPCInterfaceExampleImpl;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.InterfaceSchema;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * created by manzhang on 2021/11/12
 */
public abstract class RPCEndpointsBuilderTestBase {

    public InterfaceSchema schema = RPCEndpointsBuilder.build(getInterfaceName(), getRPCType(), new RPCInterfaceExampleImpl(), null, null, null, null, getAuthInfo(), getCustomizedValueInRequests(), specifyCustomizedNotNullAnnotation());

    public abstract String getInterfaceName();

    public abstract int expectedNumberOfEndpoints();

    public List<CustomizedRequestValueDto> getCustomizedValueInRequests(){
        return null;
    }

    public List<CustomizedNotNullAnnotationForRPCDto> specifyCustomizedNotNullAnnotation() {
        return Arrays.asList(
                new CustomizedNotNullAnnotationForRPCDto(){{
                    annotationType = "com.thrift.example.artificial.CustomAnnotation";
                    annotationMethod = "necessity";
                    equalsTo = Necessity.REQUIRED;
                }}
        );
    }

    public List<AuthenticationDto> getAuthInfo(){return null;}

    public RPCType getRPCType(){
        return RPCType.GENERAL;
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
        return params.stream().anyMatch(s-> s.getType().getFullTypeNameWithGenericType().equals(fullTypeName));
    }

    @Test
    public void testEndpointsLoad(){
        assertEquals(expectedNumberOfEndpoints(), schema.getEndpoints().size());
    }
}
