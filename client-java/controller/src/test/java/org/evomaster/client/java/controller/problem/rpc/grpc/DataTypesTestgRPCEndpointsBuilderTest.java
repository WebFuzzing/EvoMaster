package org.evomaster.client.java.controller.problem.rpc.grpc;

import io.grpc.examples.evotests.datatypes.DataTypesTestGrpc;
import io.grpc.examples.evotests.datatypes.GetInfo;
import io.grpc.examples.evotests.datatypes.SimpleObj;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.params.ObjectParam;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataTypesTestgRPCEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {
    @Override
    public String getInterfaceName() {
        return DataTypesTestGrpc.DataTypesTestBlockingStub.class.getName();
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 7;
    }

    @Test
    public void testAllFunctions(){
        List<String> expectedFunctions = Arrays.asList(
            "getSimpleObj",
            "getNestedObj",
            "setNestedObj",
            "getMapObj",
            "addMapObj",
            "getListObj",
            "addListObj"
        );

        for (String fun : expectedFunctions){
            EndpointSchema endpoint = getOneEndpoint(fun);
            assertNotNull(endpoint);
        }

    }

    @Test
    public void testGetSimpleObj() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("getSimpleObj");
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param1 = endpoint.getRequestParams().get(0);
        assertTrue(param1 instanceof ObjectParam);
        ObjectType returnType = (ObjectType)param1.getType();
        assertEquals(1, returnType.getFields().size());
        assertEquals("name", returnType.getFields().get(0).getName());
        assertEquals(String.class.getName(), returnType.getFields().get(0).getType().getFullTypeName());

        ParamDto param1Dto = param1.getDto();
        assertEquals(GetInfo.class.getName(), param1Dto.type.fullTypeNameWithGenericType);
        assertEquals(1, param1Dto.innerContent.size());
        assertEquals(String.class.getName(), param1Dto.innerContent.get(0).type.fullTypeNameWithGenericType);
        param1Dto.setNotNullValue();
        param1Dto.innerContent.get(0).stringValue = "foo";
        param1.setValueBasedOnDto(param1Dto);

        Object param1Instance = param1.newInstance();
        assertTrue(param1Instance instanceof GetInfo);
        assertEquals("foo", ((GetInfo) param1Instance).getName());

        GetInfo param1Bar = GetInfo.newBuilder().setName("bar").build();
        param1.setValueBasedOnInstance(param1Bar);

        param1Instance = param1.newInstance();
        assertTrue(param1Instance instanceof GetInfo);
        assertEquals("bar", ((GetInfo) param1Instance).getName());

        List<String> param1InstanceJava = param1.newInstanceWithJava(0);
        List<String> expectedContents = Arrays.asList(
            "io.grpc.examples.evotests.datatypes.GetInfo arg0 = null;",
            "{",
            " io.grpc.examples.evotests.datatypes.GetInfo.Builder arg0builder = io.grpc.examples.evotests.datatypes.GetInfo.newBuilder();",
            " arg0builder.setName(\"bar\");",
            " arg0 = arg0builder.build();",
            "}"
        );
        assertEquals(expectedContents.size(), param1InstanceJava.size());


        for (int i = 0; i < param1InstanceJava.size(); i++)
            assertEquals(expectedContents.get(i), param1InstanceJava.get(i));
    }

}
