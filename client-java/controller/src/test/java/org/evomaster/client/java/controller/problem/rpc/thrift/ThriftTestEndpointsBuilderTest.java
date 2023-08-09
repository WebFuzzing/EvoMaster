package org.evomaster.client.java.controller.problem.rpc.thrift;

import com.thrift.example.real.thrift.test.*;
import org.apache.thrift.TException;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * in this test, we perform testing on extracting schema, new instance and generate accordingly dto
 *
 * test RPC schema with all endpoints of ThriftTest in
 *      https://raw.githubusercontent.com/apache/thrift/master/test/ThriftTest.thrift
 * this schema contains various data types defined in Thrift
 *
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
    public void testDepth(){
        List<Class> clazz = Arrays.asList(Xtruct.class, Xtruct2.class, Insanity.class, Numberz.class, Xception.class, Xception2.class, TException.class);
        Map<String, NamedTypedValue> typedValueMap = schema.getObjParamCollections();
        assertEquals(clazz.size(), typedValueMap.size());
        assertEquals(0, typedValueMap.get(Xtruct.class.getName()).getType().depth);
        assertEquals(0, typedValueMap.get(Numberz.class.getName()).getType().depth);
        assertEquals(0, typedValueMap.get(Xception.class.getName()).getType().depth);
        assertEquals(0, typedValueMap.get(TException.class.getName()).getType().depth);

        assertEquals(1, typedValueMap.get(Xtruct2.class.getName()).getType().depth);
        assertEquals(1, typedValueMap.get(Xception2.class.getName()).getType().depth);

        assertEquals(2, typedValueMap.get(Insanity.class.getName()).getType().depth);

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
    public void testString() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testString");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof StringParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof StringParam);
        assertEquals(1, endpoint.getExceptions().size());

        String input = "foo";
        p1.setValueBasedOnInstance(input);

        assertTrue(p1.newInstance().equals(input));
        assertTrue(p1.getDto().stringValue.equals(input));
        assertEquals(RPCSupportedDataType.STRING, p1.getDto().type.type);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(1, javaCode.size());
        assertTrue(javaCode.get(0).equals("String arg0 = \"foo\";"));


        List<String> endpointJavaCode = endpoint.newInvocationWithJavaOrKotlin("res1","controller",null, SutInfoDto.OutputFormat.JAVA_JUNIT_4);
        assertEquals(5, endpointJavaCode.size());
        assertEquals("String res1 = null;", endpointJavaCode.get(0));
        assertEquals("{", endpointJavaCode.get(1));
        assertEquals(" String arg0 = \"foo\";", endpointJavaCode.get(2));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.real.thrift.test.ThriftTest$Iface\"))).testString(arg0);", endpointJavaCode.get(3));
        assertEquals("}", endpointJavaCode.get(4));

        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertEquals(\"foo\", res1);", assertionJavaCode.get(0));

    }

    @Test
    public void testBool() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testBool");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof BooleanParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof BooleanParam);
        assertEquals(1, endpoint.getExceptions().size());


        boolean input = true;
        p1.setValueBasedOnInstance(input);

        assertTrue(p1.newInstance().equals(input));
        assertTrue(p1.getDto().stringValue.equals(""+input));
        assertEquals(RPCSupportedDataType.P_BOOLEAN, p1.getDto().type.type);


        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(1, javaCode.size());
        assertTrue(javaCode.get(0).equals("boolean arg0 = true;"));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertEquals(true, res1);", assertionJavaCode.get(0));
    }

    @Test
    public void testByte() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testByte");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ByteParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ByteParam);
        assertEquals(1, endpoint.getExceptions().size());


        byte input = 42;
        p1.setValueBasedOnInstance(input);

        assertTrue(p1.newInstance().equals(input));
        assertTrue(p1.getDto().stringValue.equals(""+input));
        assertEquals(RPCSupportedDataType.P_BYTE, p1.getDto().type.type);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true,true);
        assertEquals(1, javaCode.size());
        assertTrue(javaCode.get(0).equals("byte arg0 = 42;"));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertEquals(42, res1);", assertionJavaCode.get(0));
    }

    @Test
    public void testInt() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testI32");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof IntParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof IntParam);
        assertEquals(1, endpoint.getExceptions().size());


        int input = 42;
        p1.setValueBasedOnInstance(input);

        assertTrue(p1.newInstance().equals(input));
        assertTrue(p1.getDto().stringValue.equals(""+input));
        assertEquals(RPCSupportedDataType.P_INT, p1.getDto().type.type);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(1, javaCode.size());
        assertTrue(javaCode.get(0).equals("int arg0 = 42;"));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertEquals(42, res1);", assertionJavaCode.get(0));
    }

    @Test
    public void testLong() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testI64");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof LongParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof LongParam);
        assertEquals(1, endpoint.getExceptions().size());


        long input = 42L;
        p1.setValueBasedOnInstance(input);

        assertTrue(p1.newInstance().equals(input));
        assertTrue(p1.getDto().stringValue.equals(""+input));
        assertEquals(RPCSupportedDataType.P_LONG, p1.getDto().type.type);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(1, javaCode.size());
        assertTrue(javaCode.get(0).equals("long arg0 = 42L;"));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertEquals(42L, res1);", assertionJavaCode.get(0));
    }

    @Test
    public void testDouble() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testDouble");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof DoubleParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof DoubleParam);
        assertEquals(1, endpoint.getExceptions().size());

        double input = 42.42;
        p1.setValueBasedOnInstance(input);

        assertTrue(p1.newInstance().equals(input));
        assertTrue(p1.getDto().stringValue.equals(""+input));
        assertEquals(RPCSupportedDataType.P_DOUBLE, p1.getDto().type.type);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(1, javaCode.size());
        assertTrue(javaCode.get(0).equals("double arg0 = 42.42;"));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertTrue(numbersMatch(42.42, res1));", assertionJavaCode.get(0));
    }

    @Test
    public void testBinary() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testBinary");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ByteBufferParam);
        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ByteBufferParam);
        assertEquals(1, endpoint.getExceptions().size());

        byte[] input = "foo".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        p1.setValueBasedOnInstance(buffer);

        assertTrue(p1.newInstance() instanceof ByteBuffer);
        String extracted = new String(((ByteBuffer) p1.newInstance()).array(), StandardCharsets.UTF_8);
        assertTrue(extracted.equals("foo"));
        assertTrue(p1.getDto().stringValue.equals("foo"));
        assertEquals(RPCSupportedDataType.BYTEBUFFER, p1.getDto().type.type);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(6, javaCode.size());
        assertEquals("java.nio.ByteBuffer arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" byte[] arg0_byteArray = \"foo\".getBytes(java.nio.charset.StandardCharsets.UTF_8);", javaCode.get(2));
        assertEquals(" arg0 = java.nio.ByteBuffer.allocate(arg0_byteArray.length);", javaCode.get(3));
        assertEquals(" arg0.put(arg0_byteArray);", javaCode.get(4));
        assertEquals("}", javaCode.get(5));

        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("// not handle ByteBuffer assertion", assertionJavaCode.get(0));
    }

    @Test
    public void testStruct() throws ClassNotFoundException {

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

        Xtruct input = new Xtruct();
        input.string_thing = "foo";
        input.byte_thing = 42;
        input.i32_thing = 42;
        input.i64_thing = 42L;

        request.setValueBasedOnInstance(input);
        Object instance = request.newInstance();
        assertTrue(instance instanceof  Xtruct);
        assertEquals(input.byte_thing, ((Xtruct)instance).byte_thing);
        assertEquals(input.i32_thing, ((Xtruct)instance).i32_thing);
        assertEquals(input.i64_thing, ((Xtruct)instance).i64_thing);
        assertEquals(input.string_thing, ((Xtruct)instance).string_thing);

        ParamDto dto = request.getDto();
        assertEquals(dto.type.type, RPCSupportedDataType.CUSTOM_OBJECT);
        assertEquals(4, dto.innerContent.size());
        for (ParamDto fdto : dto.innerContent){
            if (fdto.name.equals("string_thing")){
                assertEquals(input.string_thing, fdto.stringValue);
            } else if (fdto.name.equals("byte_thing")){
                assertEquals(fdto.stringValue, "" + input.byte_thing);
            } else if (fdto.name.equals("i32_thing")){
                assertEquals(fdto.stringValue, "" + input.i32_thing);
            } else if (fdto.name.equals("i64_thing")){
                assertEquals(fdto.stringValue, "" + input.i64_thing);
            } else {
                fail("error field name "+fdto.name);
            }
        }

        List<String> javaCode = request.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(8, javaCode.size());
        assertEquals("com.thrift.example.real.thrift.test.Xtruct arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" arg0 = new com.thrift.example.real.thrift.test.Xtruct();", javaCode.get(2));
        assertEquals(" arg0.setString_thing(\"foo\");", javaCode.get(3));
        assertEquals(" arg0.setByte_thing(((byte)(42)));", javaCode.get(4));
        assertEquals(" arg0.setI32_thing(42);", javaCode.get(5));
        assertEquals(" arg0.setI64_thing(42L);", javaCode.get(6));
        assertEquals("}", javaCode.get(7));


        List<String> assertionJavaCode = request.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(4, assertionJavaCode.size());
        assertEquals("assertEquals(\"foo\", res1.string_thing);", assertionJavaCode.get(0));
        assertEquals("assertEquals(42, res1.byte_thing);", assertionJavaCode.get(1));
        assertEquals("assertEquals(42, res1.i32_thing);", assertionJavaCode.get(2));
        assertEquals("assertEquals(42L, res1.i64_thing);", assertionJavaCode.get(3));

        // thrift inits the default value for primitive types
        Xtruct empty = new Xtruct();
        request.setValueBasedOnInstance(empty);

        javaCode = request.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(8, javaCode.size());
        assertEquals("com.thrift.example.real.thrift.test.Xtruct arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" arg0 = new com.thrift.example.real.thrift.test.Xtruct();", javaCode.get(2));
        assertEquals(" arg0.setByte_thing(((byte)(0)));", javaCode.get(4));
        assertEquals(" arg0.setI32_thing(0);", javaCode.get(5));
        assertEquals(" arg0.setI64_thing(0L);", javaCode.get(6));
        assertEquals("}", javaCode.get(7));


        assertionJavaCode = request.newAssertionWithJavaOrKotlin(0, "res1", -1,true );
        assertEquals(4, assertionJavaCode.size());
        assertEquals("assertNull(res1.string_thing);", assertionJavaCode.get(0));
        assertEquals("assertEquals(0, res1.byte_thing);", assertionJavaCode.get(1));
        assertEquals("assertEquals(0, res1.i32_thing);", assertionJavaCode.get(2));
        assertEquals("assertEquals(0L, res1.i64_thing);", assertionJavaCode.get(3));
    }

    @Test
    public void testNest() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testNest");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ObjectParam);
        assertEquals(Xtruct2.class.getName(), response.getType().getFullTypeName());
        List<NamedTypedValue> fields = ((ObjectType)response.getType()).getFields();
        assertEquals(3, fields.size());
        assertTrue(fields.get(0) instanceof ByteParam);
        assertTrue(fields.get(1) instanceof ObjectParam);
        assertTrue(fields.get(2) instanceof IntParam);
        List<NamedTypedValue> ifields = ((ObjectType)(fields.get(1)).getType()).getFields();
        assertEquals(4, ifields.size());

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue request = endpoint.getRequestParams().get(0);
        assertTrue(request instanceof ObjectParam);
        assertEquals(Xtruct2.class.getName(), request.getType().getFullTypeName());
        List<NamedTypedValue> rfields = ((ObjectType)request.getType()).getFields();
        assertEquals(3, rfields.size());
        assertTrue(rfields.get(0) instanceof ByteParam);
        assertTrue(rfields.get(1) instanceof ObjectParam);
        assertTrue(rfields.get(2) instanceof IntParam);
        List<NamedTypedValue> rifields = ((ObjectType)(rfields.get(1)).getType()).getFields();
        assertEquals(4, rifields.size());

        Xtruct2 input = new Xtruct2();
        input.byte_thing = 42;
        input.i32_thing = 42;
        Xtruct objField = new Xtruct();
        input.struct_thing = objField;
        objField.i64_thing = 100L;
        objField.i32_thing = 100;
        objField.byte_thing = 100;
        objField.string_thing = "bar";

        request.setValueBasedOnInstance(input);
        Object requestInstance = request.newInstance();
        assertTrue(requestInstance instanceof Xtruct2);
        Xtruct2 xinstance2 = (Xtruct2) requestInstance;
        assertEquals(input.byte_thing, xinstance2.byte_thing);
        assertEquals(input.i32_thing, xinstance2.i32_thing);
        assertEquals(objField.byte_thing, input.struct_thing.byte_thing);
        assertEquals(objField.i32_thing, input.struct_thing.i32_thing);
        assertEquals(objField.i64_thing, input.struct_thing.i64_thing);

        ParamDto requestDto = request.getDto();
        assertEquals(RPCSupportedDataType.CUSTOM_OBJECT, requestDto.type.type);
        assertEquals(3, requestDto.innerContent.size());

        for (ParamDto fdto : requestDto.innerContent){
            if (fdto.name.equals("byte_thing")){
                assertEquals(fdto.stringValue, "" + input.byte_thing);
            } else if (fdto.name.equals("i32_thing")){
                assertEquals(fdto.stringValue, "" + input.i32_thing);
            } else if (fdto.name.equals("struct_thing")){
                assertEquals(4, fdto.innerContent.size());
                for (ParamDto fidto : fdto.innerContent){
                    if (fidto.name.equals("string_thing")){
                        assertEquals(objField.string_thing, fidto.stringValue);
                    } else if (fidto.name.equals("byte_thing")){
                        assertEquals("" + objField.byte_thing, fidto.stringValue);
                    } else if (fidto.name.equals("i32_thing")){
                        assertEquals("" + objField.i32_thing, fidto.stringValue);
                    } else if (fidto.name.equals("i64_thing")){
                        assertEquals("" + objField.i64_thing, fidto.stringValue);
                    } else {
                        fail("error field name of Xtruct "+fidto.name);
                    }
                }
            } else {
                fail("error field name of Xtruct "+fdto.name);
            }
        }


        List<String> javaCode = request.newInstanceWithJavaOrKotlin(0, true, true);

        assertEquals(15, javaCode.size());
        assertEquals("com.thrift.example.real.thrift.test.Xtruct2 arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" arg0 = new com.thrift.example.real.thrift.test.Xtruct2();", javaCode.get(2));
        assertEquals(" arg0.setByte_thing(((byte)(42)));", javaCode.get(3));
        assertEquals(" com.thrift.example.real.thrift.test.Xtruct arg0_struct_thing = null;", javaCode.get(4));
        assertEquals(" {", javaCode.get(5));
        assertEquals("  arg0_struct_thing = new com.thrift.example.real.thrift.test.Xtruct();", javaCode.get(6));
        assertEquals("  arg0_struct_thing.setString_thing(\"bar\");", javaCode.get(7));
        assertEquals("  arg0_struct_thing.setByte_thing(((byte)(100)));", javaCode.get(8));
        assertEquals("  arg0_struct_thing.setI32_thing(100);", javaCode.get(9));
        assertEquals("  arg0_struct_thing.setI64_thing(100L);", javaCode.get(10));
        assertEquals(" }", javaCode.get(11));
        assertEquals(" arg0.setStruct_thing(arg0_struct_thing);", javaCode.get(12));
        assertEquals(" arg0.setI32_thing(42);", javaCode.get(13));
        assertEquals("}", javaCode.get(14));



        List<String> assertionJavaCode = request.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(6, assertionJavaCode.size());
        assertEquals("assertEquals(42, res1.byte_thing);", assertionJavaCode.get(0));
        assertEquals("assertEquals(\"bar\", res1.struct_thing.string_thing);", assertionJavaCode.get(1));
        assertEquals("assertEquals(100, res1.struct_thing.byte_thing);", assertionJavaCode.get(2));
        assertEquals("assertEquals(100, res1.struct_thing.i32_thing);", assertionJavaCode.get(3));
        assertEquals("assertEquals(100L, res1.struct_thing.i64_thing);", assertionJavaCode.get(4));
        assertEquals("assertEquals(42, res1.i32_thing);", assertionJavaCode.get(5));

    }


    @Test
    public void testMap() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testMap");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof MapParam);
        PairParam template = ((MapParam)response).getType().getTemplate();
        assertTrue(template.getType().getFirstTemplate() instanceof IntParam);
        assertTrue(template.getType().getSecondTemplate() instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof MapParam);
        assertEquals(1, endpoint.getExceptions().size());


        Map<Integer, Integer> input = new HashMap<Integer, Integer>(){{
            put(1, 2);
            put(2, 3);
            put(42, 43);
            put(100, 101);
        }};

        p1.setValueBasedOnInstance(input);
        Object instance = p1.newInstance();
        assertTrue(instance instanceof Map);
        assertEquals(4, input.size());
        assertEquals(2, input.get(1));
        assertEquals(3, input.get(2));
        assertEquals(43, input.get(42));
        assertEquals(101, input.get(100));

        ParamDto dto = p1.getDto();
        assertEquals(RPCSupportedDataType.MAP, dto.type.type);
        assertEquals(4, dto.innerContent.size());

        Map<Integer, Integer> results = new HashMap<>();
        for (ParamDto e: dto.innerContent){
            assertEquals(2, e.innerContent.size());
            results.put(Integer.parseInt(e.innerContent.get(0).stringValue), Integer.parseInt(e.innerContent.get(1).stringValue));
        }
        assertEquals(4, results.size());
        assertEquals(2, results.get(1));
        assertEquals(3, results.get(2));
        assertEquals(43, results.get(42));
        assertEquals(101, results.get(100));

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(16, javaCode.size());
        assertEquals("java.util.Map<Integer,Integer> arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" arg0 = new java.util.HashMap<>();", javaCode.get(2));
        assertEquals(" Integer arg0_key_0 = 1;", javaCode.get(3));
        assertEquals(" Integer arg0_value_0 = 2;", javaCode.get(4));
        assertEquals(" arg0.put(arg0_key_0,arg0_value_0);", javaCode.get(5));
        assertEquals(" Integer arg0_key_1 = 2;", javaCode.get(6));
        assertEquals(" Integer arg0_value_1 = 3;", javaCode.get(7));
        assertEquals(" arg0.put(arg0_key_1,arg0_value_1);", javaCode.get(8));
        assertEquals(" Integer arg0_key_2 = 100;", javaCode.get(9));
        assertEquals(" Integer arg0_value_2 = 101;", javaCode.get(10));
        assertEquals(" arg0.put(arg0_key_2,arg0_value_2);", javaCode.get(11));
        assertEquals(" Integer arg0_key_3 = 42;", javaCode.get(12));
        assertEquals(" Integer arg0_value_3 = 43;", javaCode.get(13));
        assertEquals(" arg0.put(arg0_key_3,arg0_value_3);", javaCode.get(14));
        assertEquals("}", javaCode.get(15));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(5, assertionJavaCode.size());
        assertEquals("assertEquals(4, res1.size());", assertionJavaCode.get(0));
        assertEquals("assertEquals(2, res1.get(1).intValue());", assertionJavaCode.get(1));
        assertEquals("assertEquals(3, res1.get(2).intValue());", assertionJavaCode.get(2));
        assertEquals("assertEquals(101, res1.get(100).intValue());", assertionJavaCode.get(3));
        assertEquals("assertEquals(43, res1.get(42).intValue());", assertionJavaCode.get(4));

        assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", 2, true);
        assertEquals(1+2, assertionJavaCode.size());

        assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", 0, true);
        assertEquals(1, assertionJavaCode.size());

    }

    @Test
    public void testStringMap() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testStringMap");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof MapParam);
        PairParam template = ((MapParam)response).getType().getTemplate();
        assertTrue(template.getType().getFirstTemplate() instanceof StringParam);
        assertTrue(template.getType().getSecondTemplate() instanceof StringParam);

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof MapParam);
        assertEquals(1, endpoint.getExceptions().size());

        Map<String, String> input = new HashMap<String, String>(){{
            put("foo", ""+2);
            put("bar", ""+3);
            put("abc", ""+43);
            put("def", ""+101);
        }};

        p1.setValueBasedOnInstance(input);
        Object instance = p1.newInstance();
        assertTrue(instance instanceof Map);
        assertEquals(4, input.size());
        assertEquals(""+2, input.get("foo"));
        assertEquals(""+3, input.get("bar"));
        assertEquals(""+43, input.get("abc"));
        assertEquals(""+101, input.get("def"));


        ParamDto dto = p1.getDto();
        assertEquals(RPCSupportedDataType.MAP, dto.type.type);
        assertEquals(4, dto.innerContent.size());

        Map<String, String> results = new HashMap<>();
        for (ParamDto e: dto.innerContent){
            assertEquals(2, e.innerContent.size());
            results.put(e.innerContent.get(0).stringValue, e.innerContent.get(1).stringValue);
        }
        assertEquals(4, results.size());
        assertEquals(""+2, results.get("foo"));
        assertEquals(""+3, results.get("bar"));
        assertEquals(""+43, results.get("abc"));
        assertEquals(""+101, results.get("def"));

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(16, javaCode.size());
        assertEquals("java.util.Map<String,String> arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" arg0 = new java.util.HashMap<>();", javaCode.get(2));
        assertEquals(" String arg0_key_0 = \"bar\";", javaCode.get(3));
        assertEquals(" String arg0_value_0 = \"3\";", javaCode.get(4));
        assertEquals(" arg0.put(arg0_key_0,arg0_value_0);", javaCode.get(5));
        assertEquals(" String arg0_key_1 = \"abc\";", javaCode.get(6));
        assertEquals(" String arg0_value_1 = \"43\";", javaCode.get(7));
        assertEquals(" arg0.put(arg0_key_1,arg0_value_1);", javaCode.get(8));
        assertEquals(" String arg0_key_2 = \"def\";", javaCode.get(9));
        assertEquals(" String arg0_value_2 = \"101\";", javaCode.get(10));
        assertEquals(" arg0.put(arg0_key_2,arg0_value_2);", javaCode.get(11));
        assertEquals(" String arg0_key_3 = \"foo\";", javaCode.get(12));
        assertEquals(" String arg0_value_3 = \"2\";", javaCode.get(13));
        assertEquals(" arg0.put(arg0_key_3,arg0_value_3);", javaCode.get(14));
        assertEquals("}", javaCode.get(15));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(5, assertionJavaCode.size());
        assertEquals("assertEquals(4, res1.size());", assertionJavaCode.get(0));
        assertEquals("assertEquals(\"3\", res1.get(\"bar\"));", assertionJavaCode.get(1));
        assertEquals("assertEquals(\"43\", res1.get(\"abc\"));", assertionJavaCode.get(2));
        assertEquals("assertEquals(\"101\", res1.get(\"def\"));", assertionJavaCode.get(3));
        assertEquals("assertEquals(\"2\", res1.get(\"foo\"));", assertionJavaCode.get(4));
    }

    @Test
    public void testSet() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testSet");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof SetParam);
        NamedTypedValue template = ((SetParam)response).getType().getTemplate();
        assertTrue(template instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof SetParam);
        assertEquals(1, endpoint.getExceptions().size());

        Set<Integer> input = new LinkedHashSet<Integer>(){{
            add(1);
            add(2);
            add(3);
        }};
        p1.setValueBasedOnInstance(input);

        Object instance = p1.newInstance();
        assertTrue(instance instanceof Set);
        assertEquals(3, ((Set)instance).size());
        assertTrue(((Set)instance).containsAll(input));

        ParamDto dto = p1.getDto();
        assertEquals(RPCSupportedDataType.SET, dto.type.type);
        assertEquals(RPCSupportedDataType.INT, dto.type.example.type.type);

        assertEquals(3, dto.innerContent.size());
        assertTrue(dto.innerContent.stream().allMatch(s-> input.contains(Integer.parseInt(s.stringValue))));

        List<String> javaCodes = p1.newInstanceWithJavaOrKotlin(0, true, true);

        assertEquals(10, javaCodes.size());
        assertEquals("java.util.Set<Integer> arg0 = null;", javaCodes.get(0));
        assertEquals("{", javaCodes.get(1));
        assertEquals(" arg0 = new java.util.HashSet<>();", javaCodes.get(2));
        assertEquals(" Integer arg0_e_0 = 1;", javaCodes.get(3));
        assertEquals(" arg0.add(arg0_e_0);", javaCodes.get(4));
        assertEquals(" Integer arg0_e_1 = 2;", javaCodes.get(5));
        assertEquals(" arg0.add(arg0_e_1);", javaCodes.get(6));
        assertEquals(" Integer arg0_e_2 = 3;", javaCodes.get(7));
        assertEquals(" arg0.add(arg0_e_2);", javaCodes.get(8));
        assertEquals("}", javaCodes.get(9));

        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("assertEquals(3, res1.size());", assertionJavaCode.get(0));

        assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", 2, true);
        assertEquals(1, assertionJavaCode.size());
    }

    @Test
    public void testList() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testList");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof ListParam);
        NamedTypedValue template = ((ListParam)response).getType().getTemplate();
        assertTrue(template instanceof IntParam);

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ListParam);
        assertEquals(1, endpoint.getExceptions().size());

        List<Integer> input = new ArrayList<Integer>(){{
            add(1);
            add(2);
            add(3);
        }};
        p1.setValueBasedOnInstance(input);

        Object instance = p1.newInstance();
        assertTrue(instance instanceof List);
        assertEquals(3, ((List)instance).size());
        assertTrue(((List)instance).containsAll(input));

        ParamDto dto = p1.getDto();
        assertEquals(RPCSupportedDataType.LIST, dto.type.type);
        assertEquals(RPCSupportedDataType.INT, dto.type.example.type.type);

        assertEquals(3, dto.innerContent.size());
        assertTrue(dto.innerContent.stream().allMatch(s-> input.contains(Integer.parseInt(s.stringValue))));

        List<String> javaCodes = p1.newInstanceWithJavaOrKotlin(0, true, true);

        assertEquals(10, javaCodes.size());
        assertEquals("java.util.List<Integer> arg0 = null;", javaCodes.get(0));
        assertEquals("{", javaCodes.get(1));
        assertEquals(" arg0 = new java.util.ArrayList<>();", javaCodes.get(2));
        assertEquals(" Integer arg0_e_0 = 1;", javaCodes.get(3));
        assertEquals(" arg0.add(arg0_e_0);", javaCodes.get(4));
        assertEquals(" Integer arg0_e_1 = 2;", javaCodes.get(5));
        assertEquals(" arg0.add(arg0_e_1);", javaCodes.get(6));
        assertEquals(" Integer arg0_e_2 = 3;", javaCodes.get(7));
        assertEquals(" arg0.add(arg0_e_2);", javaCodes.get(8));
        assertEquals("}", javaCodes.get(9));

        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(4, assertionJavaCode.size());
        assertEquals("assertEquals(3, res1.size());", assertionJavaCode.get(0));
        assertEquals("assertEquals(1, res1.get(0).intValue());", assertionJavaCode.get(1));
        assertEquals("assertEquals(2, res1.get(1).intValue());", assertionJavaCode.get(2));
        assertEquals("assertEquals(3, res1.get(2).intValue());", assertionJavaCode.get(3));

        assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", 2, true);
        assertEquals(1+2, assertionJavaCode.size());

        assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", 0, true);
        assertEquals(1, assertionJavaCode.size());
    }

    @Test
    public void testEnum() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("testEnum");
        NamedTypedValue response = endpoint.getResponse();
        assertNotNull(response);
        assertTrue(response instanceof EnumParam);
        String[] items = ((EnumParam)response).getType().getItems();
        assertEquals(6, items.length);
        assertTrue(Arrays.asList(items).containsAll(Arrays.asList("ONE", "TWO", "THREE", "FIVE", "SIX", "EIGHT")));

        assertEquals(1, endpoint.getRequestParams().size());
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof EnumParam);
        assertEquals(1, endpoint.getExceptions().size());

        Numberz two = Numberz.TWO;
        int index = two.ordinal();
        p1.setValueBasedOnInstance(two);
        assertEquals(index, ((EnumParam) p1).getValue());
        assertEquals(Numberz.TWO, p1.newInstance());

        ParamDto dto = p1.getDto();
        assertEquals(RPCSupportedDataType.ENUM, dto.type.type);
        assertEquals(""+index, dto.stringValue);

        List<String> javaCode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(1, javaCode.size());
        assertEquals("com.thrift.example.real.thrift.test.Numberz arg0 = com.thrift.example.real.thrift.test.Numberz.TWO;", javaCode.get(0));

        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionJavaCode.size());
        assertEquals("//assertEquals(com.thrift.example.real.thrift.test.Numberz.TWO, res1);", assertionJavaCode.get(0));
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
    public void testMapMap() throws ClassNotFoundException {
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
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof IntParam);
        assertEquals(1, endpoint.getExceptions().size());

        // check response for this method
        Map<Integer, Map<Integer, Integer>> responseObj = new HashMap<Integer, Map<Integer, Integer>>(){{
            put(1, new HashMap<Integer, Integer>(){{
                put(1,1);
                put(2,2);
            }});

            put(2, new HashMap<Integer, Integer>(){{
                put(1,2);
                put(2,4);
                put(3,6);
            }});
        }};
        response.setValueBasedOnInstance(responseObj);
        Object instance = response.newInstance();
        assertTrue(instance instanceof Map);
        assertEquals(2, ((Map<?, ?>) instance).size());
        Object key1 = ((Map<?, ?>) instance).get(1);
        assertTrue(key1 instanceof Map);
        assertEquals(2, ((Map<?, ?>) key1).size());
        assertTrue(((Map<?, ?>) key1).keySet().containsAll(responseObj.get(1).keySet()));
        assertTrue(((Map<?, ?>) key1).values().containsAll(responseObj.get(1).values()));

        Object key2 = ((Map<?, ?>) instance).get(2);
        assertTrue(key2 instanceof  Map);
        assertEquals(3, ((Map<?, ?>) key2).size());
        assertTrue(((Map<?, ?>) key2).keySet().containsAll(responseObj.get(2).keySet()));
        assertTrue(((Map<?, ?>) key2).values().containsAll(responseObj.get(2).values()));

        ParamDto dto = response.getDto();
        assertEquals(RPCSupportedDataType.MAP, dto.type.type);
        // two pair elements
        assertEquals(2, dto.innerContent.size());

        // 1st pair element
        assertEquals(2, dto.innerContent.get(0).innerContent.size());
        assertEquals(""+1, dto.innerContent.get(0).innerContent.get(0).stringValue);
        assertEquals(RPCSupportedDataType.MAP, dto.innerContent.get(0).innerContent.get(1).type.type);
        assertEquals(2, dto.innerContent.get(0).innerContent.get(1).innerContent.size());
        for (ParamDto p : dto.innerContent.get(0).innerContent.get(1).innerContent){
            assertEquals(2, p.innerContent.size());
            if (p.innerContent.get(0).stringValue.equals("1")){
                assertEquals("1", p.innerContent.get(1).stringValue);
            } else if (p.innerContent.get(0).stringValue.equals("2")){
                assertEquals("2", p.innerContent.get(1).stringValue);
            } else {
                fail("invalid key:value, ie,"+ p.innerContent.get(0).stringValue +":"+p.innerContent.get(1).stringValue);
            }
        }

        // 2nd pair element
        assertEquals(2, dto.innerContent.get(1).innerContent.size());
        assertEquals(""+2, dto.innerContent.get(1).innerContent.get(0).stringValue);
        assertEquals(RPCSupportedDataType.MAP,dto.innerContent.get(1).innerContent.get(1).type.type);
        assertEquals(3, dto.innerContent.get(1).innerContent.get(1).innerContent.size());

        for (ParamDto p : dto.innerContent.get(1).innerContent.get(1).innerContent){
            assertEquals(2, p.innerContent.size());
            if (p.innerContent.get(0).stringValue.equals("1")){
                assertEquals("2", p.innerContent.get(1).stringValue);
            } else if (p.innerContent.get(0).stringValue.equals("2")){
                assertEquals("4", p.innerContent.get(1).stringValue);
            } else if (p.innerContent.get(0).stringValue.equals("3")){
                assertEquals("6", p.innerContent.get(1).stringValue);
            } else {
                fail("invalid key:value, ie,"+ p.innerContent.get(0).stringValue +":"+p.innerContent.get(1).stringValue);
            }
        }

        List<String> assertionNullJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertionNullJavaCode.size());
        assertEquals("assertNull(res1);", assertionNullJavaCode.get(0));

        List<String> assertionJavaCode = response.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(8, assertionJavaCode.size());
        assertEquals("assertEquals(2, res1.size());", assertionJavaCode.get(0));
        assertEquals("assertEquals(2, res1.get(1).size());", assertionJavaCode.get(1));
        assertEquals("assertEquals(1, res1.get(1).get(1).intValue());", assertionJavaCode.get(2));
        assertEquals("assertEquals(2, res1.get(1).get(2).intValue());", assertionJavaCode.get(3));
        assertEquals("assertEquals(3, res1.get(2).size());", assertionJavaCode.get(4));
        assertEquals("assertEquals(2, res1.get(2).get(1).intValue());", assertionJavaCode.get(5));
        assertEquals("assertEquals(4, res1.get(2).get(2).intValue());", assertionJavaCode.get(6));
        assertEquals("assertEquals(6, res1.get(2).get(3).intValue());", assertionJavaCode.get(7));

        assertionJavaCode = response.newAssertionWithJavaOrKotlin(0, "res1", 1, true);
        // 2 set size + at most 1 check for value (there is no value at index 0)
        assertTrue(2+1 >= assertionJavaCode.size());


        assertionJavaCode = response.newAssertionWithJavaOrKotlin(0, "res1", 0, true);
        // 2 set size + at most 1 check for value (there is no value at index 0)
        assertEquals(1, assertionJavaCode.size());
    }

    @Test
    public void testInsanity() throws ClassNotFoundException {
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
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);
        assertEquals(1, endpoint.getExceptions().size());


        Map<Numberz, Long> userMap = new HashMap<Numberz, Long>(){{
            put(Numberz.TWO, 2L);
            put(Numberz.EIGHT, 8L);
            put(Numberz.SIX, 6L);
        }};
        List<Xtruct> xtructs = new ArrayList<Xtruct>(){{
            add(new Xtruct("foo",((byte)100), 100, 100L));
        }};

        Insanity input = new Insanity(userMap, xtructs);
        p1.setValueBasedOnInstance(input);
        Object instance = p1.newInstance();
        assertTrue(instance instanceof Insanity);
        assertEquals(3,  ((Insanity) instance).userMap.size());
        assertTrue(((Insanity) instance).userMap.keySet().containsAll(userMap.keySet()));
        assertTrue(((Insanity) instance).userMap.values().containsAll(userMap.values()));

        ParamDto dto = p1.getDto();
        assertEquals(RPCSupportedDataType.CUSTOM_OBJECT, dto.type.type);
        assertEquals(2, dto.innerContent.size());
        ParamDto userMapDto = dto.innerContent.get(0);
        assertEquals(RPCSupportedDataType.MAP, userMapDto.type.type);

        // check example
        assertEquals(2, userMapDto.type.example.innerContent.size());
        assertEquals(RPCSupportedDataType.ENUM, userMapDto.type.example.innerContent.get(0).type.type);
        assertEquals(RPCSupportedDataType.LONG, userMapDto.type.example.innerContent.get(1).type.type);

        assertEquals(3, userMapDto.innerContent.size());
        for (ParamDto p: userMapDto.innerContent){
            assertEquals(2, p.innerContent.size());
            ParamDto key = p.innerContent.get(0);
            ParamDto value = p.innerContent.get(1);
            assertEquals(RPCSupportedDataType.ENUM, key.type.type);
            assertEquals(RPCSupportedDataType.LONG, value.type.type);
            if (key.stringValue.equals(""+Numberz.TWO.ordinal())){
                assertEquals(""+2L, value.stringValue);
            } else if (key.stringValue.equals(""+Numberz.EIGHT.ordinal())){
                assertEquals(""+8L, value.stringValue);
            } else if (key.stringValue.equals(""+Numberz.SIX.ordinal())){
                assertEquals(""+6L, value.stringValue);
            } else {
                fail("invalid key:value, ie,"+ key.stringValue +":"+value.stringValue);
            }

        }

        ParamDto xtructsDto = dto.innerContent.get(1);
        assertEquals(1, xtructsDto.innerContent.size());
        assertEquals(RPCSupportedDataType.LIST, xtructsDto.type.type);
        ParamDto xtructDto = xtructsDto.innerContent.get(0);
        assertEquals(RPCSupportedDataType.CUSTOM_OBJECT, xtructDto.type.type);
        for (ParamDto fdto : xtructDto.innerContent){
            if (fdto.name.equals("string_thing")){
                assertEquals("foo", fdto.stringValue);
            } else if (fdto.name.equals("byte_thing")){
                assertEquals(""+((byte)100), fdto.stringValue);
            } else if (fdto.name.equals("i32_thing")){
                assertEquals(""+100, fdto.stringValue);
            } else if (fdto.name.equals("i64_thing")){
                assertEquals(""+100L, fdto.stringValue);
            } else {
                fail("error field name "+fdto.name);
            }
        }

        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(9, assertionJavaCode.size());
        // disable due to flaky
//        assertEquals("assertEquals(3, res1.userMap.size());", assertionJavaCode.get(0));
//        assertEquals("assertEquals(8L, res1.userMap.get(com.thrift.example.real.thrift.test.Numberz.EIGHT));", assertionJavaCode.get(1));
//        assertEquals("assertEquals(6L, res1.userMap.get(com.thrift.example.real.thrift.test.Numberz.SIX));", assertionJavaCode.get(2));
//        assertEquals("assertEquals(2L, res1.userMap.get(com.thrift.example.real.thrift.test.Numberz.TWO));", assertionJavaCode.get(3));
//        assertEquals("assertEquals(1, res1.xtructs.size());", assertionJavaCode.get(4));
//        assertEquals("assertEquals(\"foo\", res1.xtructs.get(0).string_thing);", assertionJavaCode.get(5));
//        assertEquals("assertEquals(100, res1.xtructs.get(0).byte_thing);", assertionJavaCode.get(6));
//        assertEquals("assertEquals(100, res1.xtructs.get(0).i32_thing);", assertionJavaCode.get(7));
//        assertEquals("assertEquals(100L, res1.xtructs.get(0).i64_thing);", assertionJavaCode.get(8));
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
