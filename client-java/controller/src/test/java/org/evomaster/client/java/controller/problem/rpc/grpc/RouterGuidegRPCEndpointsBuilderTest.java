package org.evomaster.client.java.controller.problem.rpc.grpc;

import io.grpc.examples.routeguide.RouteGuideGrpc;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.params.ObjectParam;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RouterGuidegRPCEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {
    @Override
    public String getInterfaceName() {
        return RouteGuideGrpc.RouteGuideBlockingStub.class.getName();
    }

    @Override
    public int expectedNumberOfEndpoints() {
        /*

        this  gRPC has four functions in total, one simple and three streaming apis.
        as we only support the simple RPC for the moment, then 'expectedNumberOfEndpoints' is 1 now

         */
        return 1;
    }

    @Test
    public void testProtobuf(){

        EndpointSchema endpoint = getOneEndpoint("getFeature");
        NamedTypedValue response = endpoint.getResponse();
        assertTrue(response instanceof ObjectParam);
        ObjectType returnType = ((ObjectParam)response).getType();
        assertEquals("io.grpc.examples.routeguide.Feature", returnType.getFullTypeName());
        assertEquals(2, returnType.getFields().size());
        assertEquals("name", returnType.getFields().get(0).getName());
        assertEquals(String.class.getName(), returnType.getFields().get(0).getType().getFullTypeName());

        assertEquals("location", returnType.getFields().get(1).getName());
        assertEquals("io.grpc.examples.routeguide.Point", returnType.getFields().get(1).getType().getFullTypeName());


        List<NamedTypedValue> params = endpoint.getRequestParams();
        assertEquals(1, params.size());
        NamedTypedValue inputParam = params.get(0);
        assertTrue(inputParam instanceof ObjectParam);
        ObjectType paramType = ((ObjectParam) inputParam).getType();
        assertEquals("io.grpc.examples.routeguide.Point", paramType.getFullTypeName());
        assertEquals(2, paramType.getFields().size());
        assertEquals("latitude", paramType.getFields().get(0).getName());
        assertEquals(int.class.getName(), paramType.getFields().get(0).getType().getFullTypeName());
        assertEquals("longitude", paramType.getFields().get(1).getName());
        assertEquals(int.class.getName(), paramType.getFields().get(1).getType().getFullTypeName());
    }
}
