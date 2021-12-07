package org.evomaster.client.java.controller.problem.rpc.thrift;

import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test RPC schema parser with all endpoints of ThriftTest in
 *      https://raw.githubusercontent.com/apache/thrift/master/test/ThriftTest.thrift
 */
public class ThriftTestEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {

    @Override
    public String getInterfaceName() {
        return "com.thrift.example.real.thrift.test.ThriftTest$Iface";
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 22;
    }

    @Test
    public void testEndpointsLoad(){
        assertEquals(expectedNumberOfEndpoints(), schema.getEndpoints().size());
    }

    @Test
    public void testAllContainTException(){
        boolean all = schema.getEndpoints().stream().allMatch(e-> containType(e.getExceptions(),"org.apache.thrift.TException" ));
        assertTrue(all);
    }

    @Test
    public void testVoid(){
        EndpointSchema endpoint = getOneEndpoint("testVoid");
        NamedTypedValue response = endpoint.getResponse();
        assertNull(response);
        assertTrue(endpoint.getRequestParams().isEmpty());
        assertEquals(1, endpoint.getExceptions().size());
    }


    @Test
    public void testString(){
        EndpointSchema endpoint = getOneEndpoint("testString");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof StringParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof StringParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testBool(){
        EndpointSchema endpoint = getOneEndpoint("testBool");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof BooleanParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof BooleanParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testByte(){
        EndpointSchema endpoint = getOneEndpoint("testByte");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ByteParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof ByteParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testInt(){
        EndpointSchema endpoint = getOneEndpoint("testI32");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof IntParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof IntParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testLong(){
        EndpointSchema endpoint = getOneEndpoint("testI64");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof LongParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof LongParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testDouble(){
        EndpointSchema endpoint = getOneEndpoint("testDouble");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof DoubleParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof DoubleParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testBinary(){
        EndpointSchema endpoint = getOneEndpoint("testBinary");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ByteBufferParam);
        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof ByteBufferParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testStruct(){
        EndpointSchema endpoint = getOneEndpoint("testStruct");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ObjectParam);
        assertEquals("com.thrift.example.real.thrift.test.Xtruct", response.getType().getFullTypeName());
        List<NamedTypedValue> fields = ((ObjectType)response.getType()).getFields();
        assertEquals(4, fields.size());
        assertTrue(fields.get(0) instanceof StringParam);
        assertTrue(fields.get(1) instanceof ByteParam);
        assertTrue(fields.get(2) instanceof IntParam);
        assertTrue(fields.get(3) instanceof LongParam);

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue request = endpoint.getRequestParams().get(0);
        assertTrue(request instanceof ObjectParam);
        assertEquals("com.thrift.example.real.thrift.test.Xtruct", request.getType().getFullTypeName());
    }

    @Test
    public void testNest(){
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


    @Test
    public void testMap(){
        EndpointSchema endpoint = getOneEndpoint("testMap");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof MapParam);
        PairParam template = ((MapParam)response).getType().getTemplate();
        assertTrue(template.getType().getFirstTemplate() instanceof IntParam);
        assertTrue(template.getType().getSecondTemplate() instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof MapParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testStringMap(){
        EndpointSchema endpoint = getOneEndpoint("testStringMap");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof MapParam);
        PairParam template = ((MapParam)response).getType().getTemplate();
        assertTrue(template.getType().getFirstTemplate() instanceof StringParam);
        assertTrue(template.getType().getSecondTemplate() instanceof StringParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof MapParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testSet(){
        EndpointSchema endpoint = getOneEndpoint("testSet");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof SetParam);
        NamedTypedValue template = ((SetParam)response).getType().getTemplate();
        assertTrue(template instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof SetParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testList(){
        EndpointSchema endpoint = getOneEndpoint("testList");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ListParam);
        NamedTypedValue template = ((ListParam)response).getType().getTemplate();
        assertTrue(template instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof ListParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testEnum(){
        EndpointSchema endpoint = getOneEndpoint("testEnum");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof EnumParam);
        String[] items = ((EnumParam)response).getType().getItems();
        assertEquals(6, items.length);
        assertTrue(Arrays.asList(items).containsAll(Arrays.asList("ONE", "TWO", "THREE", "FIVE", "SIX", "EIGHT")));

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof EnumParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testTypedef(){
        EndpointSchema endpoint = getOneEndpoint("testTypedef");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof LongParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof LongParam);
        assertEquals(1, endpoint.getExceptions().size());
    }


    @Test
    public void testMapMap(){
        EndpointSchema endpoint = getOneEndpoint("testMapMap");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof MapParam);
        PairParam template = ((MapParam)response).getType().getTemplate();
        assertTrue(template.getType().getFirstTemplate() instanceof IntParam);
        NamedTypedValue mapValue = template.getType().getSecondTemplate();
        assertTrue(mapValue instanceof MapParam);
        PairParam templateMapValue=((MapParam)mapValue).getType().getTemplate();
        assertTrue(templateMapValue.getType().getFirstTemplate() instanceof IntParam);
        assertTrue(templateMapValue.getType().getSecondTemplate() instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof IntParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testInsanity(){
        EndpointSchema endpoint = getOneEndpoint("testInsanity");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof MapParam);
        PairParam template = ((MapParam)response).getType().getTemplate();
        assertTrue(template.getType().getFirstTemplate() instanceof LongParam);
        NamedTypedValue mapValue = template.getType().getSecondTemplate();
        assertTrue(mapValue instanceof MapParam);
        PairParam templateMapValue=((MapParam)mapValue).getType().getTemplate();
        assertTrue(templateMapValue.getType().getFirstTemplate() instanceof EnumParam);
        assertTrue(templateMapValue.getType().getSecondTemplate() instanceof ObjectParam);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof ObjectParam);
        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testMulti(){
        EndpointSchema endpoint = getOneEndpoint("testMulti");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ObjectParam);

        assertEquals(6, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof ByteParam);
        assertTrue(endpoint.getRequestParams().get(1) instanceof IntParam);
        assertTrue(endpoint.getRequestParams().get(2) instanceof LongParam);

        NamedTypedValue mapValue = endpoint.getRequestParams().get(3);
        assertTrue(mapValue instanceof MapParam);
        PairParam templateMapValue=((MapParam)mapValue).getType().getTemplate();
        assertTrue(templateMapValue.getType().getFirstTemplate() instanceof ShortParam);
        assertTrue(templateMapValue.getType().getSecondTemplate() instanceof StringParam);

        assertTrue(endpoint.getRequestParams().get(4) instanceof EnumParam);
        assertTrue(endpoint.getRequestParams().get(5) instanceof LongParam);

        assertEquals(1, endpoint.getExceptions().size());
    }

    @Test
    public void testException(){
        EndpointSchema endpoint = getOneEndpoint("testException");
        NamedTypedValue response = endpoint.getResponse();
        assertNull(response);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof StringParam);
        assertEquals(2, endpoint.getExceptions().size());
        assertTrue(containType(endpoint.getExceptions(), "com.thrift.example.real.thrift.test.Xception"));
    }

    @Test
    public void testMultiException(){
        EndpointSchema endpoint = getOneEndpoint("testMultiException");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ObjectParam);

        assertEquals(2, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof StringParam);
        assertTrue(endpoint.getRequestParams().get(1) instanceof StringParam);
        assertEquals(3, endpoint.getExceptions().size());
        assertTrue(containType(endpoint.getExceptions(), "com.thrift.example.real.thrift.test.Xception"));
        assertTrue(containType(endpoint.getExceptions(), "com.thrift.example.real.thrift.test.Xception2"));

    }

    @Test
    public void testOneway(){
        EndpointSchema endpoint = getOneEndpoint("testOneway");
        NamedTypedValue response = endpoint.getResponse();
        assertNull(response);

        assertEquals(1, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof IntParam);
        assertEquals(1, endpoint.getExceptions().size());
    }
}
