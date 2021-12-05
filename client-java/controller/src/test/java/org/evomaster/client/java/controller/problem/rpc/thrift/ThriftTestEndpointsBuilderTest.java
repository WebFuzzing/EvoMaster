package org.evomaster.client.java.controller.problem.rpc.thrift;

import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.EnumType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * created by manzhang on 2021/11/12
 */
public class ThriftTestEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {

    @Override
    public String getInterfaceName() {
        return "com.thrift.example.real.thrift.test.ThriftTest$Iface";
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 21;
    }

    @Test
    public void testEndpointsLoad(){
        assertEquals(expectedNumberOfEndpoints(), schema.getEndpoints().size());
    }

    @Test
    public void testObject(){
        EndpointSchema endpoint = getOneEndpoint("testNest");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ObjectParam);
        assertEquals("com.thrift.example.real.thrift.test.Xtruct2", response.getType().getFullTypeName());
        List<NamedTypedValue> fields = ((ObjectType)response.getType()).getFields();
        assertEquals(3, fields.size());
        assertTrue(fields.get(0) instanceof ByteParam);
        assertTrue(fields.get(1) instanceof ObjectParam);
        assertTrue(fields.get(2) instanceof IntParam);

        List<NamedTypedValue> ifields = ((ObjectType)(fields.get(1)).getType()).getFields();
        assertEquals(4, ifields.size());
    }

}
