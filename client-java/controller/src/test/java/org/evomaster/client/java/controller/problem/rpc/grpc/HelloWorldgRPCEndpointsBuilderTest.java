package org.evomaster.client.java.controller.problem.rpc.grpc;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloRequest;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.params.ObjectParam;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelloWorldgRPCEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {
    @Override
    public String getInterfaceName() {
        return GreeterGrpc.GreeterBlockingStub.class.getName();
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 1;
    }

    @Test
    public void testProtobuf(){

        EndpointSchema endpoint = getOneEndpoint("sayHello");
        NamedTypedValue response = endpoint.getResponse();
        assertTrue(response instanceof ObjectParam);
        ObjectType returnType = (ObjectType)response.getType();
        assertEquals(1, returnType.getFields().size());
        assertEquals("message", returnType.getFields().get(0).getName());
        assertEquals(String.class.getName(), returnType.getFields().get(0).getType().getFullTypeName());


        List<NamedTypedValue> params = endpoint.getRequestParams();
        assertEquals(1, params.size());
        NamedTypedValue inputParam = params.get(0);
        assertTrue(inputParam instanceof ObjectParam);
        ObjectType paramType = ((ObjectParam) inputParam).getType();
        assertEquals(1, paramType.getFields().size());
        assertEquals("name", paramType.getFields().get(0).getName());
        assertEquals(String.class.getName(), paramType.getFields().get(0).getType().getFullTypeName());
    }
}
