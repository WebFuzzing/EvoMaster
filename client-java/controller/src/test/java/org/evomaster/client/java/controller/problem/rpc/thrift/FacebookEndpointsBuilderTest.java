package org.evomaster.client.java.controller.problem.rpc.thrift;

import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.EnumParam;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.types.EnumType;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * created by manzhang on 2021/11/12
 */
public class FacebookEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {

    @Override
    public String getInterfaceName() {
        return "com.thrift.example.real.facebook.fb303.FacebookService$Iface";
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 13;
    }



    @Test
    public void testEnum(){
        EndpointSchema endpoint = getOneEndpoint("getStatus");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof EnumParam);
        assertEquals("com.thrift.example.real.facebook.fb303.fb_status", response.getType().getFullTypeName());
        assertEquals(6, ((EnumType)response.getType()).getItems().length);
    }

}
