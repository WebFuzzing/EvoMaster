package org.evomaster.client.java.controller.problem.rpc.grpc;

import com.google.protobuf.ByteString;
import io.grpc.examples.evotests.datatypes.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.params.ObjectParam;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.Protobuf3ByteStringType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public void testGetInfo() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("getSimpleObj");
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param1 = endpoint.getRequestParams().get(0);
        assertTrue(param1 instanceof ObjectParam);
        ObjectType param1Type = (ObjectType)param1.getType();
        assertEquals(1, param1Type.getFields().size());
        assertEquals("name", param1Type.getFields().get(0).getName());
        assertEquals(String.class.getName(), param1Type.getFields().get(0).getType().getFullTypeName());

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

        List<String> param1InstanceJava = param1.newInstanceWithJavaOrKotlin(0, true, true);
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

    @Test
    public void testSimpleObj() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("setNestedObj");
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue param1 = endpoint.getRequestParams().get(0);
        assertTrue(param1 instanceof ObjectParam);
        ObjectType param1Type = (ObjectType)param1.getType();
        assertEquals(8, param1Type.getFields().size());
        assertEquals("name", param1Type.getFields().get(0).getName());
        assertEquals(String.class.getName(), param1Type.getFields().get(0).getType().getFullTypeName());
        assertEquals("doublevalue", param1Type.getFields().get(1).getName());
        assertEquals(double.class.getName(), param1Type.getFields().get(1).getType().getFullTypeName());
        assertEquals("int32Value", param1Type.getFields().get(2).getName());
        assertEquals(int.class.getName(), param1Type.getFields().get(2).getType().getFullTypeName());
        assertEquals("int64Value", param1Type.getFields().get(3).getName());
        assertEquals(long.class.getName(), param1Type.getFields().get(3).getType().getFullTypeName());
        assertEquals("floatvalue", param1Type.getFields().get(4).getName());
        assertEquals(float.class.getName(), param1Type.getFields().get(4).getType().getFullTypeName());
        assertEquals("boolvalue", param1Type.getFields().get(5).getName());
        assertEquals(boolean.class.getName(), param1Type.getFields().get(5).getType().getFullTypeName());
        assertEquals("bytesvalue", param1Type.getFields().get(6).getName());
        assertEquals(Protobuf3ByteStringType.PROTOBUF3_BYTE_STRING_TYPE_NAME, param1Type.getFields().get(6).getType().getFullTypeName());
        assertEquals("enumvalue", param1Type.getFields().get(7).getName());
        assertEquals(SimpleEnum.class.getName(), param1Type.getFields().get(7).getType().getFullTypeName());


        SimpleObj objInstance = SimpleObj
            .newBuilder()
            .setName("foo")
            .setDoublevalue(0.42)
            .setInt32Value(42)
            .setInt64Value(42L)
            .setFloatvalue(0.42f)
            .setBoolvalue(false)
            .setBytesvalue(ByteString.copyFromUtf8("helloworld"))
            .setEnumvalue(SimpleEnum.ONE)
            .build();

        param1.setValueBasedOnInstance(objInstance);

        Object param1Instance = param1.newInstance();
        assertTrue(param1Instance instanceof SimpleObj);
        assertEquals(42, ((SimpleObj) param1Instance).getInt32Value());


        List<String> param1InstanceJava = param1.newInstanceWithJavaOrKotlin(0, true, true);
        String[] expectedContents = ("io.grpc.examples.evotests.datatypes.SimpleObj arg0 = null;\n" +
            "{\n" +
            " io.grpc.examples.evotests.datatypes.SimpleObj.Builder arg0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();\n" +
            " arg0builder.setName(\"foo\");\n" +
            " arg0builder.setDoublevalue(0.42);\n" +
            " arg0builder.setInt32Value(42);\n" +
            " arg0builder.setInt64Value(42L);\n" +
            " arg0builder.setFloatvalue(0.42f);\n" +
            " arg0builder.setBoolvalue(false);\n" +
            " com.google.protobuf.ByteString arg0_bytesvalue = null;\n" +
            " {\n" +
            "  arg0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8(\"helloworld\");\n" +
            " }\n" +
            " arg0builder.setBytesvalue(arg0_bytesvalue);\n" +
            " arg0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));\n" +
            " arg0 = arg0builder.build();\n" +
            "}").split("\n");

        assertEquals(expectedContents.length, param1InstanceJava.size());

        for (int i = 0; i < param1InstanceJava.size(); i++)
            assertEquals(expectedContents[i], param1InstanceJava.get(i));

    }

    @Test
    public void testNestedObj() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("setNestedObj");
        NamedTypedValue response = endpoint.getResponse();
        assertTrue(response instanceof ObjectParam);
        ObjectType responseType = (ObjectType)response.getType();
        assertEquals(2, responseType.getFields().size());
        assertEquals("name", responseType.getFields().get(0).getName());
        assertEquals(String.class.getName(), responseType.getFields().get(0).getType().getFullTypeName());
        assertEquals("objvalue", responseType.getFields().get(1).getName());
        assertEquals(SimpleObj.class.getName(), responseType.getFields().get(1).getType().getFullTypeName());


        SimpleObj objInstance = SimpleObj
            .newBuilder()
            .setName("foo")
            .setDoublevalue(0.42)
            .setInt32Value(42)
            .setInt64Value(42L)
            .setFloatvalue(0.42f)
            .setBoolvalue(false)
            .setBytesvalue(ByteString.copyFromUtf8("helloworld"))
            .setEnumvalue(SimpleEnum.ONE)
            .build();

        NestedObj nestedObjInstance = NestedObj
            .newBuilder()
            .setName("bar")
            .setObjvalue(objInstance).build();

        response.setValueBasedOnInstance(nestedObjInstance);

        Object param1Instance = response.newInstance();
        assertTrue(param1Instance instanceof NestedObj);
        assertEquals(42, ((NestedObj) param1Instance).getObjvalue().getInt32Value());


        List<String> param1InstanceJava = response.newInstanceWithJavaOrKotlin(true, true, "res", 0, true, true);

        String[] expectedContents = ("io.grpc.examples.evotests.datatypes.NestedObj res = null;\n" +
            "{\n" +
            " io.grpc.examples.evotests.datatypes.NestedObj.Builder resbuilder = io.grpc.examples.evotests.datatypes.NestedObj.newBuilder();\n" +
            " resbuilder.setName(\"bar\");\n" +
            " io.grpc.examples.evotests.datatypes.SimpleObj res_objvalue = null;\n" +
            " {\n" +
            "  io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_objvaluebuilder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();\n" +
            "  res_objvaluebuilder.setName(\"foo\");\n" +
            "  res_objvaluebuilder.setDoublevalue(0.42);\n" +
            "  res_objvaluebuilder.setInt32Value(42);\n" +
            "  res_objvaluebuilder.setInt64Value(42L);\n" +
            "  res_objvaluebuilder.setFloatvalue(0.42f);\n" +
            "  res_objvaluebuilder.setBoolvalue(false);\n" +
            "  com.google.protobuf.ByteString res_objvalue_bytesvalue = null;\n" +
            "  {\n" +
            "   res_objvalue_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8(\"helloworld\");\n" +
            "  }\n" +
            "  res_objvaluebuilder.setBytesvalue(res_objvalue_bytesvalue);\n" +
            "  res_objvaluebuilder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));\n" +
            "  res_objvalue = res_objvaluebuilder.build();\n" +
            " }\n" +
            " resbuilder.setObjvalue(res_objvalue);\n" +
            " res = resbuilder.build();\n" +
            "}").split("\n");

        assertEquals(expectedContents.length, param1InstanceJava.size());

        for (int i = 0; i < param1InstanceJava.size(); i++)
            assertEquals(expectedContents[i], param1InstanceJava.get(i));

        io.grpc.examples.evotests.datatypes.NestedObj res = null;
        {
            io.grpc.examples.evotests.datatypes.NestedObj.Builder resbuilder = io.grpc.examples.evotests.datatypes.NestedObj.newBuilder();
            resbuilder.setName("bar");
            io.grpc.examples.evotests.datatypes.SimpleObj res_objvalue = null;
            {
                io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_objvaluebuilder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();
                res_objvaluebuilder.setName("foo");
                res_objvaluebuilder.setDoublevalue(0.42);
                res_objvaluebuilder.setInt32Value(42);
                res_objvaluebuilder.setInt64Value(42L);
                res_objvaluebuilder.setFloatvalue(0.42f);
                res_objvaluebuilder.setBoolvalue(false);
                com.google.protobuf.ByteString res_objvalue_bytesvalue = null;
                {
                    res_objvalue_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8("helloworld");
                }
                res_objvaluebuilder.setBytesvalue(res_objvalue_bytesvalue);
                res_objvaluebuilder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));
                res_objvalue = res_objvaluebuilder.build();
            }
            resbuilder.setObjvalue(res_objvalue);
            res = resbuilder.build();
        }
    }


    @Test
    public void testMapObj() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("getMapObj");
        NamedTypedValue response = endpoint.getResponse();
        assertTrue(response instanceof ObjectParam);
        ObjectType responseType = (ObjectType)response.getType();
        assertEquals(3, responseType.getFields().size());
        assertEquals("name", responseType.getFields().get(0).getName());
        assertEquals(String.class.getName(), responseType.getFields().get(0).getType().getFullTypeName());
        assertEquals("intkeymapvalue", responseType.getFields().get(1).getName());
        assertEquals(Map.class.getName(), responseType.getFields().get(1).getType().getFullTypeName());
        assertEquals("stringkeymapvalue", responseType.getFields().get(2).getName());
        assertEquals(Map.class.getName(), responseType.getFields().get(2).getType().getFullTypeName());


        SimpleObj int1ObjInstance = SimpleObj
            .newBuilder()
            .setName("int1Foo")
            .setDoublevalue(0.42)
            .setInt32Value(42)
            .setInt64Value(42L)
            .setFloatvalue(0.42f)
            .setBoolvalue(false)
            .setBytesvalue(ByteString.copyFromUtf8("helloworld"))
            .setEnumvalue(SimpleEnum.ONE)
            .build();

        SimpleObj strbarObjInstance = SimpleObj
            .newBuilder()
            .setName("strbarFoo")
            .setDoublevalue(0.42)
            .setInt32Value(42)
            .setInt64Value(42L)
            .setFloatvalue(0.42f)
            .setBoolvalue(false)
            .setBytesvalue(ByteString.copyFromUtf8("helloworld"))
            .setEnumvalue(SimpleEnum.TWO)
            .build();

        MapObj mapObjInstance = MapObj
            .newBuilder()
            .setName("bar")
            .putIntkeymapvalue(1, int1ObjInstance)
            .putStringkeymapvalue("strbar", strbarObjInstance)
            .build();


        response.setValueBasedOnInstance(mapObjInstance);

        Object param1Instance = response.newInstance();
        assertTrue(param1Instance instanceof MapObj);
        assertEquals(42, ((MapObj) param1Instance).getIntkeymapvalueMap().get(1).getInt32Value());


        List<String> param1InstanceJava = response.newInstanceWithJavaOrKotlin(true, true, "res", 0, true, true);

        String[] expectedContents = ("io.grpc.examples.evotests.datatypes.MapObj res = null;\n" +
            "{\n" +
            " io.grpc.examples.evotests.datatypes.MapObj.Builder resbuilder = io.grpc.examples.evotests.datatypes.MapObj.newBuilder();\n" +
            " resbuilder.setName(\"bar\");\n" +
            " java.util.Map<Integer,io.grpc.examples.evotests.datatypes.SimpleObj> res_intkeymapvalue = null;\n" +
            " {\n" +
            "  res_intkeymapvalue = new java.util.HashMap<>();\n" +
            "  Integer res_intkeymapvalue_key_0 = 1;\n" +
            "  io.grpc.examples.evotests.datatypes.SimpleObj res_intkeymapvalue_value_0 = null;\n" +
            "  {\n" +
            "   io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_intkeymapvalue_value_0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();\n" +
            "   res_intkeymapvalue_value_0builder.setName(\"int1Foo\");\n" +
            "   res_intkeymapvalue_value_0builder.setDoublevalue(0.42);\n" +
            "   res_intkeymapvalue_value_0builder.setInt32Value(42);\n" +
            "   res_intkeymapvalue_value_0builder.setInt64Value(42L);\n" +
            "   res_intkeymapvalue_value_0builder.setFloatvalue(0.42f);\n" +
            "   res_intkeymapvalue_value_0builder.setBoolvalue(false);\n" +
            "   com.google.protobuf.ByteString res_intkeymapvalue_value_0_bytesvalue = null;\n" +
            "   {\n" +
            "    res_intkeymapvalue_value_0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8(\"helloworld\");\n" +
            "   }\n" +
            "   res_intkeymapvalue_value_0builder.setBytesvalue(res_intkeymapvalue_value_0_bytesvalue);\n" +
            "   res_intkeymapvalue_value_0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));\n" +
            "   res_intkeymapvalue_value_0 = res_intkeymapvalue_value_0builder.build();\n" +
            "  }\n" +
            "  res_intkeymapvalue.put(res_intkeymapvalue_key_0,res_intkeymapvalue_value_0);\n" +
            " }\n" +
            " resbuilder.putAllIntkeymapvalue(res_intkeymapvalue);\n" +
            " java.util.Map<String,io.grpc.examples.evotests.datatypes.SimpleObj> res_stringkeymapvalue = null;\n" +
            " {\n" +
            "  res_stringkeymapvalue = new java.util.HashMap<>();\n" +
            "  String res_stringkeymapvalue_key_0 = \"strbar\";\n" +
            "  io.grpc.examples.evotests.datatypes.SimpleObj res_stringkeymapvalue_value_0 = null;\n" +
            "  {\n" +
            "   io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_stringkeymapvalue_value_0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();\n" +
            "   res_stringkeymapvalue_value_0builder.setName(\"strbarFoo\");\n" +
            "   res_stringkeymapvalue_value_0builder.setDoublevalue(0.42);\n" +
            "   res_stringkeymapvalue_value_0builder.setInt32Value(42);\n" +
            "   res_stringkeymapvalue_value_0builder.setInt64Value(42L);\n" +
            "   res_stringkeymapvalue_value_0builder.setFloatvalue(0.42f);\n" +
            "   res_stringkeymapvalue_value_0builder.setBoolvalue(false);\n" +
            "   com.google.protobuf.ByteString res_stringkeymapvalue_value_0_bytesvalue = null;\n" +
            "   {\n" +
            "    res_stringkeymapvalue_value_0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8(\"helloworld\");\n" +
            "   }\n" +
            "   res_stringkeymapvalue_value_0builder.setBytesvalue(res_stringkeymapvalue_value_0_bytesvalue);\n" +
            "   res_stringkeymapvalue_value_0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.TWO)));\n" +
            "   res_stringkeymapvalue_value_0 = res_stringkeymapvalue_value_0builder.build();\n" +
            "  }\n" +
            "  res_stringkeymapvalue.put(res_stringkeymapvalue_key_0,res_stringkeymapvalue_value_0);\n" +
            " }\n" +
            " resbuilder.putAllStringkeymapvalue(res_stringkeymapvalue);\n" +
            " res = resbuilder.build();\n" +
            "}").split("\n");

        assertEquals(expectedContents.length, param1InstanceJava.size());

        for (int i = 0; i < param1InstanceJava.size(); i++)
            assertEquals(expectedContents[i], param1InstanceJava.get(i));

        io.grpc.examples.evotests.datatypes.MapObj res = null;
        {
            io.grpc.examples.evotests.datatypes.MapObj.Builder resbuilder = io.grpc.examples.evotests.datatypes.MapObj.newBuilder();
            resbuilder.setName("bar");
            java.util.Map<Integer,io.grpc.examples.evotests.datatypes.SimpleObj> res_intkeymapvalue = null;
            {
                res_intkeymapvalue = new java.util.HashMap<>();
                Integer res_intkeymapvalue_key_0 = 1;
                io.grpc.examples.evotests.datatypes.SimpleObj res_intkeymapvalue_value_0 = null;
                {
                    io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_intkeymapvalue_value_0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();
                    res_intkeymapvalue_value_0builder.setName("int1Foo");
                    res_intkeymapvalue_value_0builder.setDoublevalue(0.42);
                    res_intkeymapvalue_value_0builder.setInt32Value(42);
                    res_intkeymapvalue_value_0builder.setInt64Value(42L);
                    res_intkeymapvalue_value_0builder.setFloatvalue(0.42f);
                    res_intkeymapvalue_value_0builder.setBoolvalue(false);
                    com.google.protobuf.ByteString res_intkeymapvalue_value_0_bytesvalue = null;
                    {
                        res_intkeymapvalue_value_0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8("helloworld");
                    }
                    res_intkeymapvalue_value_0builder.setBytesvalue(res_intkeymapvalue_value_0_bytesvalue);
                    res_intkeymapvalue_value_0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));
                    res_intkeymapvalue_value_0 = res_intkeymapvalue_value_0builder.build();
                }
                res_intkeymapvalue.put(res_intkeymapvalue_key_0,res_intkeymapvalue_value_0);
            }
            resbuilder.putAllIntkeymapvalue(res_intkeymapvalue);
            java.util.Map<String,io.grpc.examples.evotests.datatypes.SimpleObj> res_stringkeymapvalue = null;
            {
                res_stringkeymapvalue = new java.util.HashMap<>();
                String res_stringkeymapvalue_key_0 = "strbar";
                io.grpc.examples.evotests.datatypes.SimpleObj res_stringkeymapvalue_value_0 = null;
                {
                    io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_stringkeymapvalue_value_0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();
                    res_stringkeymapvalue_value_0builder.setName("strbarFoo");
                    res_stringkeymapvalue_value_0builder.setDoublevalue(0.42);
                    res_stringkeymapvalue_value_0builder.setInt32Value(42);
                    res_stringkeymapvalue_value_0builder.setInt64Value(42L);
                    res_stringkeymapvalue_value_0builder.setFloatvalue(0.42f);
                    res_stringkeymapvalue_value_0builder.setBoolvalue(false);
                    com.google.protobuf.ByteString res_stringkeymapvalue_value_0_bytesvalue = null;
                    {
                        res_stringkeymapvalue_value_0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8("helloworld");
                    }
                    res_stringkeymapvalue_value_0builder.setBytesvalue(res_stringkeymapvalue_value_0_bytesvalue);
                    res_stringkeymapvalue_value_0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.TWO)));
                    res_stringkeymapvalue_value_0 = res_stringkeymapvalue_value_0builder.build();
                }
                res_stringkeymapvalue.put(res_stringkeymapvalue_key_0,res_stringkeymapvalue_value_0);
            }
            resbuilder.putAllStringkeymapvalue(res_stringkeymapvalue);
            res = resbuilder.build();
        }
    }


    @Test
    public void testListObj() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("addListObj");
        NamedTypedValue response = endpoint.getResponse();
        assertTrue(response instanceof ObjectParam);
        ObjectType responseType = (ObjectType)response.getType();
        assertEquals(4, responseType.getFields().size());
        assertEquals("name", responseType.getFields().get(0).getName());
        assertEquals(String.class.getName(), responseType.getFields().get(0).getType().getFullTypeName());
        assertEquals("intlistvalue", responseType.getFields().get(1).getName());
        assertEquals(List.class.getName(), responseType.getFields().get(1).getType().getFullTypeName());
        assertEquals("stringlistvalue", responseType.getFields().get(2).getName());
        assertEquals(List.class.getName(), responseType.getFields().get(2).getType().getFullTypeName());
        assertEquals("objlistvalue", responseType.getFields().get(3).getName());
        assertEquals(List.class.getName(), responseType.getFields().get(3).getType().getFullTypeName());

        SimpleObj int1ObjInstance = SimpleObj
            .newBuilder()
            .setName("int1Foo")
            .setDoublevalue(0.42)
            .setInt32Value(42)
            .setInt64Value(42L)
            .setFloatvalue(0.42f)
            .setBoolvalue(false)
            .setBytesvalue(ByteString.copyFromUtf8("helloworld-foo"))
            .setEnumvalue(SimpleEnum.ONE)
            .build();

        SimpleObj strbarObjInstance = SimpleObj
            .newBuilder()
            .setName("strbarFoo")
            .setDoublevalue(0.42)
            .setInt32Value(42)
            .setInt64Value(42L)
            .setFloatvalue(0.42f)
            .setBoolvalue(false)
            .setBytesvalue(ByteString.copyFromUtf8("helloworld-bar"))
            .setEnumvalue(SimpleEnum.TWO)
            .build();

        ListObj listObj = ListObj
            .newBuilder()
            .setName("bar")
            .addIntlistvalue(42)
            .addStringlistvalue("barInList")
            .addStringlistvalue("fooInList")
            .addObjlistvalue(int1ObjInstance)
            .addObjlistvalue(strbarObjInstance)
            .build();


        response.setValueBasedOnInstance(listObj);

        Object param1Instance = response.newInstance();
        assertTrue(param1Instance instanceof ListObj);
        ListObj param1InstanceValue = (ListObj) param1Instance;
        assertEquals(1, param1InstanceValue.getIntlistvalueList().size());
        assertEquals(2, param1InstanceValue.getStringlistvalueList().size());
        assertEquals(2, param1InstanceValue.getObjlistvalueList().size());


        List<String> param1InstanceJava = response.newInstanceWithJavaOrKotlin(true, true, "res", 0, true, true);

        String[] expectedContents = ("io.grpc.examples.evotests.datatypes.ListObj res = null;\n" +
            "{\n" +
            " io.grpc.examples.evotests.datatypes.ListObj.Builder resbuilder = io.grpc.examples.evotests.datatypes.ListObj.newBuilder();\n" +
            " resbuilder.setName(\"bar\");\n" +
            " java.util.List<Integer> res_intlistvalue = null;\n" +
            " {\n" +
            "  res_intlistvalue = new java.util.ArrayList<>();\n" +
            "  Integer res_intlistvalue_e_0 = 42;\n" +
            "  res_intlistvalue.add(res_intlistvalue_e_0);\n" +
            " }\n" +
            " resbuilder.addAllIntlistvalue(res_intlistvalue);\n" +
            " java.util.List<String> res_stringlistvalue = null;\n" +
            " {\n" +
            "  res_stringlistvalue = new java.util.ArrayList<>();\n" +
            "  String res_stringlistvalue_e_0 = \"barInList\";\n" +
            "  res_stringlistvalue.add(res_stringlistvalue_e_0);\n" +
            "  String res_stringlistvalue_e_1 = \"fooInList\";\n" +
            "  res_stringlistvalue.add(res_stringlistvalue_e_1);\n" +
            " }\n" +
            " resbuilder.addAllStringlistvalue(res_stringlistvalue);\n" +
            " java.util.List<io.grpc.examples.evotests.datatypes.SimpleObj> res_objlistvalue = null;\n" +
            " {\n" +
            "  res_objlistvalue = new java.util.ArrayList<>();\n" +
            "  io.grpc.examples.evotests.datatypes.SimpleObj res_objlistvalue_e_0 = null;\n" +
            "  {\n" +
            "   io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_objlistvalue_e_0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();\n" +
            "   res_objlistvalue_e_0builder.setName(\"int1Foo\");\n" +
            "   res_objlistvalue_e_0builder.setDoublevalue(0.42);\n" +
            "   res_objlistvalue_e_0builder.setInt32Value(42);\n" +
            "   res_objlistvalue_e_0builder.setInt64Value(42L);\n" +
            "   res_objlistvalue_e_0builder.setFloatvalue(0.42f);\n" +
            "   res_objlistvalue_e_0builder.setBoolvalue(false);\n" +
            "   com.google.protobuf.ByteString res_objlistvalue_e_0_bytesvalue = null;\n" +
            "   {\n" +
            "    res_objlistvalue_e_0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8(\"helloworld-foo\");\n" +
            "   }\n" +
            "   res_objlistvalue_e_0builder.setBytesvalue(res_objlistvalue_e_0_bytesvalue);\n" +
            "   res_objlistvalue_e_0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));\n" +
            "   res_objlistvalue_e_0 = res_objlistvalue_e_0builder.build();\n" +
            "  }\n" +
            "  res_objlistvalue.add(res_objlistvalue_e_0);\n" +
            "  io.grpc.examples.evotests.datatypes.SimpleObj res_objlistvalue_e_1 = null;\n" +
            "  {\n" +
            "   io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_objlistvalue_e_1builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();\n" +
            "   res_objlistvalue_e_1builder.setName(\"strbarFoo\");\n" +
            "   res_objlistvalue_e_1builder.setDoublevalue(0.42);\n" +
            "   res_objlistvalue_e_1builder.setInt32Value(42);\n" +
            "   res_objlistvalue_e_1builder.setInt64Value(42L);\n" +
            "   res_objlistvalue_e_1builder.setFloatvalue(0.42f);\n" +
            "   res_objlistvalue_e_1builder.setBoolvalue(false);\n" +
            "   com.google.protobuf.ByteString res_objlistvalue_e_1_bytesvalue = null;\n" +
            "   {\n" +
            "    res_objlistvalue_e_1_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8(\"helloworld-bar\");\n" +
            "   }\n" +
            "   res_objlistvalue_e_1builder.setBytesvalue(res_objlistvalue_e_1_bytesvalue);\n" +
            "   res_objlistvalue_e_1builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.TWO)));\n" +
            "   res_objlistvalue_e_1 = res_objlistvalue_e_1builder.build();\n" +
            "  }\n" +
            "  res_objlistvalue.add(res_objlistvalue_e_1);\n" +
            " }\n" +
            " resbuilder.addAllObjlistvalue(res_objlistvalue);\n" +
            " res = resbuilder.build();\n" +
            "}").split("\n");

        assertEquals(expectedContents.length, param1InstanceJava.size());

        for (int i = 0; i < param1InstanceJava.size(); i++)
            assertEquals(expectedContents[i], param1InstanceJava.get(i));


        io.grpc.examples.evotests.datatypes.ListObj res = null;
        {
            io.grpc.examples.evotests.datatypes.ListObj.Builder resbuilder = io.grpc.examples.evotests.datatypes.ListObj.newBuilder();
            resbuilder.setName("bar");
            java.util.List<Integer> res_intlistvalue = null;
            {
                res_intlistvalue = new java.util.ArrayList<>();
                Integer res_intlistvalue_e_0 = 42;
                res_intlistvalue.add(res_intlistvalue_e_0);
            }
            resbuilder.addAllIntlistvalue(res_intlistvalue);
            java.util.List<String> res_stringlistvalue = null;
            {
                res_stringlistvalue = new java.util.ArrayList<>();
                String res_stringlistvalue_e_0 = "barInList";
                res_stringlistvalue.add(res_stringlistvalue_e_0);
                String res_stringlistvalue_e_1 = "fooInList";
                res_stringlistvalue.add(res_stringlistvalue_e_1);
            }
            resbuilder.addAllStringlistvalue(res_stringlistvalue);
            java.util.List<io.grpc.examples.evotests.datatypes.SimpleObj> res_objlistvalue = null;
            {
                res_objlistvalue = new java.util.ArrayList<>();
                io.grpc.examples.evotests.datatypes.SimpleObj res_objlistvalue_e_0 = null;
                {
                    io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_objlistvalue_e_0builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();
                    res_objlistvalue_e_0builder.setName("int1Foo");
                    res_objlistvalue_e_0builder.setDoublevalue(0.42);
                    res_objlistvalue_e_0builder.setInt32Value(42);
                    res_objlistvalue_e_0builder.setInt64Value(42L);
                    res_objlistvalue_e_0builder.setFloatvalue(0.42f);
                    res_objlistvalue_e_0builder.setBoolvalue(false);
                    com.google.protobuf.ByteString res_objlistvalue_e_0_bytesvalue = null;
                    {
                        res_objlistvalue_e_0_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8("helloworld-foo");
                    }
                    res_objlistvalue_e_0builder.setBytesvalue(res_objlistvalue_e_0_bytesvalue);
                    res_objlistvalue_e_0builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.ONE)));
                    res_objlistvalue_e_0 = res_objlistvalue_e_0builder.build();
                }
                res_objlistvalue.add(res_objlistvalue_e_0);
                io.grpc.examples.evotests.datatypes.SimpleObj res_objlistvalue_e_1 = null;
                {
                    io.grpc.examples.evotests.datatypes.SimpleObj.Builder res_objlistvalue_e_1builder = io.grpc.examples.evotests.datatypes.SimpleObj.newBuilder();
                    res_objlistvalue_e_1builder.setName("strbarFoo");
                    res_objlistvalue_e_1builder.setDoublevalue(0.42);
                    res_objlistvalue_e_1builder.setInt32Value(42);
                    res_objlistvalue_e_1builder.setInt64Value(42L);
                    res_objlistvalue_e_1builder.setFloatvalue(0.42f);
                    res_objlistvalue_e_1builder.setBoolvalue(false);
                    com.google.protobuf.ByteString res_objlistvalue_e_1_bytesvalue = null;
                    {
                        res_objlistvalue_e_1_bytesvalue = com.google.protobuf.ByteString.copyFromUtf8("helloworld-bar");
                    }
                    res_objlistvalue_e_1builder.setBytesvalue(res_objlistvalue_e_1_bytesvalue);
                    res_objlistvalue_e_1builder.setEnumvalue(((io.grpc.examples.evotests.datatypes.SimpleEnum)(io.grpc.examples.evotests.datatypes.SimpleEnum.TWO)));
                    res_objlistvalue_e_1 = res_objlistvalue_e_1builder.build();
                }
                res_objlistvalue.add(res_objlistvalue_e_1);
            }
            resbuilder.addAllObjlistvalue(res_objlistvalue);
            res = resbuilder.build();
        }

    }
}
