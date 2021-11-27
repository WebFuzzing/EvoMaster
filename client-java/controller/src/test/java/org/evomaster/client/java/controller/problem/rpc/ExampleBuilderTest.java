package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.*;
import org.evomaster.client.java.controller.problem.RPCType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * created by manzhang on 2021/11/12
 */
public class ExampleBuilderTest extends RPCEndpointsBuilderTestBase {

    @Override
    public String getInterfaceName() {
        return "com.thrift.example.artificial.RPCInterfaceExample";
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 6;
    }

    @Override
    public RPCType getRPCType() {
        return RPCType.GENERAL;
    }

    @Test
    public void testEndpointsLoad(){
        assertEquals(expectedNumberOfEndpoints(), schema.getEndpoints().size());
    }

    @Test
    public void testArray(){

        EndpointSchema endpoint = getOneEndpoint("array");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param = endpoint.getRequestParams().get(0);
        assertTrue(param instanceof ArrayParam);
        assertTrue(param.getType() instanceof CollectionType);
        NamedTypedValue template = ((CollectionType) param.getType()).getTemplate();
        assertTrue(template instanceof ListParam);
        assertTrue(template.getType() instanceof CollectionType);
        assertTrue(((CollectionType) template.getType()).getTemplate() instanceof StringParam);
        System.out.println(param.getDto());

    }

    @Test
    public void testArrayBoolean(){

        EndpointSchema endpoint = getOneEndpoint("arrayboolean");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param = endpoint.getRequestParams().get(0);
        assertTrue(param instanceof ArrayParam);
        assertTrue(param.getType() instanceof CollectionType);
        NamedTypedValue template = ((CollectionType) param.getType()).getTemplate();
        assertTrue(template instanceof BooleanParam);
    }

    @Test
    public void testList(){

        EndpointSchema endpoint = getOneEndpoint("list");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param = endpoint.getRequestParams().get(0);
        assertTrue(param instanceof ListParam);
        assertTrue(param.getType() instanceof CollectionType);
        NamedTypedValue template = ((CollectionType) param.getType()).getTemplate();
        assertTrue(template instanceof StringParam);

    }

    @Test
    public void testMap(){

        EndpointSchema endpoint = getOneEndpoint("map");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param = endpoint.getRequestParams().get(0);
        assertTrue(param instanceof MapParam);
        assertTrue(param.getType() instanceof MapType);

        NamedTypedValue pairTemplate = ((MapType) param.getType()).getTemplate();
        assertTrue(pairTemplate instanceof PairParam);

        NamedTypedValue ktemplate = ((PairType) pairTemplate.getType()).getFirstTemplate();
        assertTrue(ktemplate instanceof StringParam);

        NamedTypedValue vtemplate = ((PairType) pairTemplate.getType()).getSecondTemplate();
        assertTrue(vtemplate instanceof StringParam);

    }

    @Test
    public void testListAndMap(){

        EndpointSchema endpoint = getOneEndpoint("listAndMap");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param = endpoint.getRequestParams().get(0);

        assertTrue(param instanceof ListParam);
        assertTrue(param.getType() instanceof CollectionType);
        NamedTypedValue mapTemplate = ((CollectionType) param.getType()).getTemplate();

        assertTrue(mapTemplate instanceof MapParam);
        assertTrue(mapTemplate.getType() instanceof MapType);

        NamedTypedValue pairTemplate = ((MapType) mapTemplate.getType()).getTemplate();
        assertTrue(pairTemplate instanceof PairParam);

        NamedTypedValue ktemplate = ((PairType) pairTemplate.getType()).getFirstTemplate();
        assertTrue(ktemplate instanceof StringParam);

        NamedTypedValue vtemplate = ((PairType) pairTemplate.getType()).getSecondTemplate();
        assertTrue(vtemplate instanceof StringParam);

    }


    @Test
    public void testObjResponse(){

        EndpointSchema endpoint = getOneEndpoint("objResponse");
        assertEquals(0, endpoint.getRequestParams().size());

        assertNotNull(endpoint.getResponse());
        NamedTypedValue param = endpoint.getResponse();
        assertTrue(param instanceof ObjectParam);
        assertTrue(param.getType() instanceof ObjectType);

        List<NamedTypedValue> fs = ((ObjectType) param.getType()).getFields();
        assertEquals(6, fs.size());
        assertTrue(fs.get(0) instanceof StringParam);
        assertTrue(fs.get(1) instanceof IntParam);
        assertTrue(fs.get(2) instanceof DoubleParam);
        assertTrue(fs.get(3) instanceof ObjectParam);
        assertTrue(fs.get(4) instanceof ArrayParam);
        assertTrue(fs.get(5) instanceof ArrayParam);

        ObjectParam fs3 = (ObjectParam) fs.get(3);
        assertTrue(fs3.getType().getFields().get(3).getType() instanceof CycleObjectType);

    }

}
