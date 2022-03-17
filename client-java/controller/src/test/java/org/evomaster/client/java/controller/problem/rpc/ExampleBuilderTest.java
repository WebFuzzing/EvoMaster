package org.evomaster.client.java.controller.problem.rpc;

import com.thrift.example.artificial.*;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.glassfish.jersey.server.model.Suspendable;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        return 26;
    }

    @Override
    public RPCType getRPCType() {
        return RPCType.GENERAL;
    }

    @Override
    public List<AuthenticationDto> getAuthInfo() {
        return Arrays.asList(
                new AuthenticationDto() {{
                    name = "foo";
                    jsonAuthEndpoint = new JsonAuthRPCEndpointDto() {{
                        endpointName = "login";
                        interfaceName = RPCInterfaceExample.class.getName();
                        jsonPayloads = Arrays.asList(
                                "{\n" +
                                        "\"id\":\"foo\",\n" +
                                        "\"passcode\":\"zXQV47zsrjfJRnTD\"\n" +
                                        "}"
                        );
                        classNames = Arrays.asList(
                                AuthLoginDto.class.getName()
                        );
                    }};
                }},
                new AuthenticationDto() {{
                    name = "bar";
                    jsonAuthEndpoint = new JsonAuthRPCEndpointDto() {{
                        endpointName = "login";
                        interfaceName = RPCInterfaceExample.class.getName();
                        jsonPayloads = Arrays.asList(
                                "{\n" +
                                        "\"id\":\"bar\",\n" +
                                        "\"passcode\":\"5jbNvXvaejDG5MhS\"\n" +
                                        "}"
                        );
                        classNames = Arrays.asList(
                                AuthLoginDto.class.getName()
                        );
                    }};
                }}
        );
    }

    @Override
    public List<CustomizedRequestValueDto> getCustomizedValueInRequests() {
        return Arrays.asList(
                new CustomizedRequestValueDto() {{
                    specificEndpointName = "handleCustomizedRequestA";
                    combinedKeyValuePairs = Arrays.asList(
                            new KeyValuePairDto() {{
                                fieldKey = "id";
                                fieldValue = "foo";
                            }},
                            new KeyValuePairDto() {{
                                fieldKey = "passcode";
                                fieldValue = "foo_passcode";
                            }}
                    );
                }},
                new CustomizedRequestValueDto() {{
                    specificEndpointName = "handleCustomizedRequestA";
                    combinedKeyValuePairs = Arrays.asList(
                            new KeyValuePairDto() {{
                                fieldKey = "id";
                                fieldValue = "bar";
                            }},
                            new KeyValuePairDto() {{
                                fieldKey = "passcode";
                                fieldValue = "bar_passcode";
                            }}
                    );
                }},
                new CustomizedRequestValueDto() {{
                    specificEndpointName = "handleCustomizedRequestB";
                    keyValues = new KeyValuesDto() {{
                        key = "value";
                        values = Arrays.asList("0.42", "42.42", "100.42");
                    }};
                }}
        );
    }

    @Test
    public void testNestedGeneric() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("handleNestedGenericString");
        assertNotNull(endpoint.getResponse());
        assertNotNull(endpoint.getRequestParams());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);
        assertEquals(3, ((ObjectParam)p1).getType().getFields().size());
        assertNull(((ObjectParam)p1).getValue());
        Object p1Instance = p1.newInstance();
        assertNull(p1Instance);
        List<String> testScript = p1.newInstanceWithJava(0);
        ParamDto dto  = p1.getDto();
        dto.innerContent = null;
        p1.setValueBasedOnDto(dto);
        List<String> testScriptWithDto =  p1.newInstanceWithJava(0);
        assertEquals(testScript, testScriptWithDto);
    }

    @Test
    public void testLocalAuthSetup(){
        EndpointSchema endpoint = getOneEndpoint("authorizedEndpoint");
        assertTrue(endpoint.getRequestParams() == null || endpoint.getRequestParams().isEmpty());
        assertNotNull(endpoint.getResponse());
        RPCActionDto dto = endpoint.getDto();
    }

    @Test
    public void testEndpointsLoad() {
        assertEquals(expectedNumberOfEndpoints(), schema.getEndpoints().size());
    }

    @Test
    public void testAuthValueSetup() throws ClassNotFoundException {
        assertNotNull(schema.getAuthEndpoints());
        assertEquals(2, schema.getAuthEndpoints().size());

        for (Map.Entry<Integer, EndpointSchema> entry : schema.getAuthEndpoints().entrySet()){
            EndpointSchema endpointSchema = entry.getValue();
            assertEquals(1, endpointSchema.getRequestParams().size());
            NamedTypedValue p1 = endpointSchema.getRequestParams().get(0);
            assertTrue(p1 instanceof ObjectParam);
            Object p1Instance = p1.newInstance();
            assertTrue(p1Instance instanceof AuthLoginDto);
            if (entry.getKey().equals(0)){
                assertEquals("foo", ((AuthLoginDto) p1Instance).id);
                assertEquals("zXQV47zsrjfJRnTD", ((AuthLoginDto) p1Instance).passcode);
            }else if (entry.getKey().equals(1)){
                assertEquals("bar", ((AuthLoginDto) p1Instance).id);
                assertEquals("5jbNvXvaejDG5MhS", ((AuthLoginDto) p1Instance).passcode);
            }
        }
    }

    @Test
    public void testHandleCustomizedRequestA() {
        EndpointSchema endpoint = getOneEndpoint("handleCustomizedRequestA");
        assertNotNull(endpoint.getRelatedCustomizedCandidates());
        assertEquals(2, endpoint.getRelatedCustomizedCandidates().size());
        assertTrue(endpoint.getRelatedCustomizedCandidates().containsAll(Arrays.asList("0", "1")));
        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);
        assertTrue(p1.isNullable());
        assertEquals(3, ((ObjectParam) p1).getType().getFields().size());

        for (NamedTypedValue f : ((ObjectParam) p1).getType().getFields()) {
            if (f.getName().equals("value")) {
                assertTrue(f instanceof IntParam);
                assertNull(((IntParam) f).getMin());
                assertNull(((IntParam) f).getMax());
            } else if (f.getName().equals("id")) {
                assertTrue(f instanceof StringParam);
                assertNotNull(f.getCandidateReferences());
                assertNotNull(f.getCandidates());
                assertEquals(2, f.getCandidates().size());
                assertEquals(2, f.getCandidateReferences().size());
                IntStream.range(0, 1).forEach(i -> {
                    assertTrue(f.getCandidates().get(i) instanceof StringParam);
                    String value = ((StringParam) f.getCandidates().get(i)).getValue();
                    if (f.getCandidateReferences().get(i).equals("0")) {
                        assertEquals("foo", value);
                    } else if (f.getCandidateReferences().get(i).equals("1")) {
                        assertEquals("bar", value);
                    }
                });
            } else if (f.getName().equals("passcode")) {
                assertTrue(f instanceof StringParam);
                assertNotNull(f.getCandidateReferences());
                assertNotNull(f.getCandidates());
                assertEquals(2, f.getCandidates().size());
                assertEquals(2, f.getCandidateReferences().size());
                IntStream.range(0, 1).forEach(i -> {
                    assertTrue(f.getCandidates().get(i) instanceof StringParam);
                    String value = ((StringParam) f.getCandidates().get(i)).getValue();
                    if (f.getCandidateReferences().get(i).equals("0")) {
                        assertEquals("foo_passcode", value);
                    } else if (f.getCandidateReferences().get(i).equals("1")) {
                        assertEquals("bar_passcode", value);
                    }
                });
            } else
                fail("do not handle param " + f.getName());
        }
    }

    @Test
    public void testHandleCustomizedRequestB() {
        EndpointSchema endpoint = getOneEndpoint("handleCustomizedRequestB");

        assertNotNull(endpoint.getRelatedCustomizedCandidates());
        assertEquals(0, endpoint.getRelatedCustomizedCandidates().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);
        assertTrue(p1.isNullable());
        assertEquals(1, ((ObjectParam) p1).getType().getFields().size());

        NamedTypedValue f = ((ObjectParam) p1).getType().getFields().get(0);
        assertNull(f.getCandidateReferences());
        assertNotNull(f.getCandidates());

        assertEquals(3, f.getCandidates().size());
        List<Double> candidates = Arrays.asList(0.42, 42.42, 100.42);
        IntStream.range(0, 2).forEach(i -> {
            assertTrue(f.getCandidates().get(i) instanceof DoubleParam);
            Double value = ((DoubleParam) f.getCandidates().get(i)).getValue();
            assertTrue(candidates.contains(value));
        });
    }

    @Test
    public void testConstraintInputs() {
        EndpointSchema endpoint = getOneEndpoint("constraintInputs");

        assertNotNull(endpoint.getResponse());
        assertEquals(2, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        checkConstrainedRequest(p1);
        checkConstrainedRequest(p1.copyStructureWithProperties());

        NamedTypedValue p2 = endpoint.getRequestParams().get(1);
        assertTrue(p2 instanceof StringParam);
        assertFalse(p2.isNullable());

        ConstrainedRequest input = new ConstrainedRequest();
        p1.setValueBasedOnInstance(input);
        ParamDto dto = p1.getDto();
        dto.innerContent.get(1).stringValue = null;
        dto.innerContent.get(2).stringValue = null;
        p1.setValueBasedOnDto(dto);
        List<String> javaCode  = p1.newInstanceWithJava(0);
        assertEquals(13, javaCode.size());
        assertEquals("com.thrift.example.artificial.ConstrainedRequest arg0 = null;", javaCode.get(0));
        assertEquals("{", javaCode.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.ConstrainedRequest();", javaCode.get(2));
        assertEquals(" arg0.list = null;", javaCode.get(3));
        assertEquals(" arg0.notBlankString = null;", javaCode.get(4));
        assertEquals(" arg0.nullableString = null;", javaCode.get(5));
        assertEquals(" arg0.stringSize = null;", javaCode.get(6));
        assertEquals(" arg0.listSize = null;", javaCode.get(7));
        assertEquals(" arg0.kind = null;", javaCode.get(8));
        assertEquals(" arg0.date = null;", javaCode.get(9));
        assertEquals(" arg0.longWithDecimalMinMax = 0L;", javaCode.get(10));
        assertEquals(" arg0.longWithInclusiveFDecimalMainMax = null;", javaCode.get(11));
        assertEquals("}", javaCode.get(12));
    }

    private void checkConstrainedRequest(NamedTypedValue p){
        assertTrue(p instanceof ObjectParam);
        assertTrue(p.isNullable());
        assertEquals(11, ((ObjectParam) p).getType().getFields().size());
        for (NamedTypedValue f : ((ObjectParam) p).getType().getFields()) {
            if (f.getName().equals("list")) {
                assertTrue(f instanceof ListParam);
                assertFalse(f.isNullable());
                assertEquals(1, ((ListParam) f).getMinSize());
            } else if (f.getName().equals("listSize")) {
                assertTrue(f instanceof ListParam);
                assertEquals(1, ((ListParam) f).getMinSize());
                assertEquals(10, ((ListParam) f).getMaxSize());
                assertFalse(f.isNullable());
            } else if (f.getName().equals("intWithMinMax")) {
                assertTrue(f instanceof IntParam);
                assertEquals(0, ((IntParam) f).getMin().intValue());
                assertEquals(100, ((IntParam) f).getMax().intValue());
                assertFalse(f.isNullable());
            } else if (f.getName().equals("longWithMinMax")) {
                assertTrue(f instanceof LongParam);
                assertEquals(-100L, ((LongParam) f).getMin());
                assertEquals(1000L, ((LongParam) f).getMax());
                assertFalse(f.isNullable());
            } else if (f.getName().equals("notBlankString")) {
                assertTrue(f instanceof StringParam);
                assertFalse(f.isNullable());
                assertEquals(1, ((StringParam) f).getMinSize());
            } else if (f.getName().equals("nullableString")) {
                assertTrue(f instanceof StringParam);
                assertTrue(f.isNullable());
            } else if (f.getName().equals("stringSize")) {
                assertTrue(f instanceof StringParam);
                assertEquals(2, ((StringParam) f).getMinSize());
                assertEquals(10, ((StringParam) f).getMaxSize());
                assertTrue(f.isNullable());
            } else if(f.getName().equals("kind")){
                assertTrue(f instanceof EnumParam);
                assertFalse(f.isNullable());
            } else if(f.getName().equals("date")){
                assertTrue(f instanceof  StringParam);
                assertTrue(f.isNullable());
                assertNotNull(((StringParam) f).getPattern());
            }else if(f.getName().equals("longWithDecimalMinMax")){
                assertTrue(f instanceof LongParam);
                assertEquals(1L, ((LongParam) f).getMin());
                assertEquals(10L, ((LongParam) f).getMax());
                assertFalse(f.isNullable());
            }else if(f.getName().equals("longWithInclusiveFDecimalMainMax")){
                assertTrue(f instanceof LongParam);
                assertEquals(2L, ((LongParam) f).getMin());
                assertEquals(9L, ((LongParam) f).getMax());
                assertTrue(f.isNullable());
            }else
                fail("do not handle param " + f.getName());
        }
    }

    @Test
    public void testAccessFieldDtoCheck(){

        EndpointSchema endpoint = getOneEndpoint("accessFieldDtoCheck");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);

        assertTrue(p1.getType() instanceof ObjectType);

        List<NamedTypedValue> fs = ((ObjectType) p1.getType()).getFields();
        assertEquals(6, fs.size());

        for (NamedTypedValue f: fs){
            if (f.getName().equals("pubField")){
                assertTrue(f.accessibleSchema.isAccessible);
            }else if (f.getName().equals("priField")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriField", f.accessibleSchema.setterMethodName);
                assertEquals("getPriField", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("stringList")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setStringList", f.accessibleSchema.setterMethodName);
                assertEquals("getStringList", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priEnum")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriEnum", f.accessibleSchema.setterMethodName);
                assertEquals("getPriEnum", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priBoolean")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriBoolean", f.accessibleSchema.setterMethodName);
                assertEquals("getPriBoolean", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("pribool")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPribool", f.accessibleSchema.setterMethodName);
                assertEquals("isPribool", f.accessibleSchema.getterMethodName);
            }
        }

        PrivateFieldInRequestDto p1Instance = new PrivateFieldInRequestDto(){{
            pubField = "foo";
            setPriField("bar");
            setStringList(Arrays.asList("1","2","3"));
            setPriEnum(EnumKind.ONE);
            setPriBoolean(true);
        }};

        p1.setValueBasedOnInstance(p1Instance);

        List<String> javaCodes = p1.newInstanceWithJava(0);

        assertEquals(20, javaCodes.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInRequestDto arg0 = null;", javaCodes.get(0));
        assertEquals("{", javaCodes.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodes.get(2));
        assertEquals(" arg0.pubField = \"foo\";", javaCodes.get(3));
        assertEquals(" arg0.setPriField(((java.lang.String)(\"bar\")));", javaCodes.get(4));
        assertEquals(" java.util.List<java.lang.String> arg0_stringList = null;", javaCodes.get(5));
        assertEquals(" {", javaCodes.get(6));
        assertEquals("  arg0_stringList = new java.util.ArrayList<>();", javaCodes.get(7));
        assertEquals("  java.lang.String arg0_stringList_e_0 = \"1\";", javaCodes.get(8));
        assertEquals("  arg0_stringList.add(arg0_stringList_e_0);", javaCodes.get(9));
        assertEquals("  java.lang.String arg0_stringList_e_1 = \"2\";", javaCodes.get(10));
        assertEquals("  arg0_stringList.add(arg0_stringList_e_1);", javaCodes.get(11));
        assertEquals("  java.lang.String arg0_stringList_e_2 = \"3\";", javaCodes.get(12));
        assertEquals("  arg0_stringList.add(arg0_stringList_e_2);", javaCodes.get(13));
        assertEquals(" }", javaCodes.get(14));
        assertEquals(" arg0.setStringList(arg0_stringList);", javaCodes.get(15));
        assertEquals(" arg0.setPriEnum(((com.thrift.example.artificial.EnumKind)(com.thrift.example.artificial.EnumKind.ONE)));", javaCodes.get(16));
        assertEquals(" arg0.setPriBoolean(((java.lang.Boolean)(true)));", javaCodes.get(17));
        assertEquals(" arg0.setPribool(((boolean)(false)));", javaCodes.get(18));
        assertEquals("}", javaCodes.get(19));


        List<String> assertionJavaCode = p1.newAssertionWithJava(0, "res1", -1);
        assertEquals(9, assertionJavaCode.size());
        assertEquals("assertEquals(\"foo\", res1.pubField);", assertionJavaCode.get(0));
        assertEquals("assertEquals(\"bar\", res1.getPriField());", assertionJavaCode.get(1));
        assertEquals("assertEquals(3, res1.getStringList().size());", assertionJavaCode.get(2));
        assertEquals("assertEquals(\"1\", res1.getStringList().get(0));", assertionJavaCode.get(3));
        assertEquals("assertEquals(\"2\", res1.getStringList().get(1));", assertionJavaCode.get(4));
        assertEquals("assertEquals(\"3\", res1.getStringList().get(2));", assertionJavaCode.get(5));
        assertEquals("assertEquals(com.thrift.example.artificial.EnumKind.ONE, res1.getPriEnum());", assertionJavaCode.get(6));
        assertEquals("assertEquals(true, res1.getPriBoolean().booleanValue());", assertionJavaCode.get(7));
        assertEquals("assertEquals(false, res1.isPribool());", assertionJavaCode.get(8));

        NamedTypedValue res = endpoint.getResponse();
        PrivateFieldInResponseDto resInstance = new PrivateFieldInResponseDto(){{
            pubField = 42;
            setPriRequest(p1Instance);
        }};

        res.setValueBasedOnInstance(resInstance);
        List<String> javaCodesForResponse = res.newInstanceWithJava(true, true, "tmp", 0);
        assertEquals(26, javaCodesForResponse.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInResponseDto tmp = null;", javaCodesForResponse.get(0));
        assertEquals("{", javaCodesForResponse.get(1));
        assertEquals(" tmp = new com.thrift.example.artificial.PrivateFieldInResponseDto();", javaCodesForResponse.get(2));
        assertEquals(" tmp.pubField = 42;", javaCodesForResponse.get(3));
        assertEquals(" com.thrift.example.artificial.PrivateFieldInRequestDto tmp_priRequest = null;", javaCodesForResponse.get(4));
        assertEquals(" {", javaCodesForResponse.get(5));
        assertEquals("  tmp_priRequest = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodesForResponse.get(6));
        assertEquals("  tmp_priRequest.pubField = \"foo\";", javaCodesForResponse.get(7));
        assertEquals("  tmp_priRequest.setPriField(((java.lang.String)(\"bar\")));", javaCodesForResponse.get(8));
        assertEquals("  java.util.List<java.lang.String> tmp_priRequest_stringList = null;", javaCodesForResponse.get(9));
        assertEquals("  {", javaCodesForResponse.get(10));
        assertEquals("   tmp_priRequest_stringList = new java.util.ArrayList<>();", javaCodesForResponse.get(11));
        assertEquals("   java.lang.String tmp_priRequest_stringList_e_0 = \"1\";", javaCodesForResponse.get(12));
        assertEquals("   tmp_priRequest_stringList.add(tmp_priRequest_stringList_e_0);", javaCodesForResponse.get(13));
        assertEquals("   java.lang.String tmp_priRequest_stringList_e_1 = \"2\";", javaCodesForResponse.get(14));
        assertEquals("   tmp_priRequest_stringList.add(tmp_priRequest_stringList_e_1);", javaCodesForResponse.get(15));
        assertEquals("   java.lang.String tmp_priRequest_stringList_e_2 = \"3\";", javaCodesForResponse.get(16));
        assertEquals("   tmp_priRequest_stringList.add(tmp_priRequest_stringList_e_2);", javaCodesForResponse.get(17));
        assertEquals("  }", javaCodesForResponse.get(18));
        assertEquals("  tmp_priRequest.setStringList(tmp_priRequest_stringList);", javaCodesForResponse.get(19));
        assertEquals("  tmp_priRequest.setPriEnum(((com.thrift.example.artificial.EnumKind)(com.thrift.example.artificial.EnumKind.ONE)));", javaCodesForResponse.get(20));
        assertEquals("  tmp_priRequest.setPriBoolean(((java.lang.Boolean)(true)));", javaCodesForResponse.get(21));
        assertEquals("  tmp_priRequest.setPribool(((boolean)(false)));", javaCodesForResponse.get(22));
        assertEquals(" }", javaCodesForResponse.get(23));
        assertEquals(" tmp.setPriRequest(tmp_priRequest);", javaCodesForResponse.get(24));
        assertEquals("}", javaCodesForResponse.get(25));

        List<String> assertionJavaCodeForResponse = res.newAssertionWithJava(0, "res1", -1);
        assertEquals(10, assertionJavaCodeForResponse.size());
        assertEquals("assertEquals(42, res1.pubField);", assertionJavaCodeForResponse.get(0));
        assertEquals("assertEquals(\"foo\", res1.getPriRequest().pubField);", assertionJavaCodeForResponse.get(1));
        assertEquals("assertEquals(\"bar\", res1.getPriRequest().getPriField());", assertionJavaCodeForResponse.get(2));
        assertEquals("assertEquals(3, res1.getPriRequest().getStringList().size());", assertionJavaCodeForResponse.get(3));
        assertEquals("assertEquals(\"1\", res1.getPriRequest().getStringList().get(0));", assertionJavaCodeForResponse.get(4));
        assertEquals("assertEquals(\"2\", res1.getPriRequest().getStringList().get(1));", assertionJavaCodeForResponse.get(5));
        assertEquals("assertEquals(\"3\", res1.getPriRequest().getStringList().get(2));", assertionJavaCodeForResponse.get(6));
        assertEquals("assertEquals(com.thrift.example.artificial.EnumKind.ONE, res1.getPriRequest().getPriEnum());", assertionJavaCodeForResponse.get(7));
        assertEquals("assertEquals(true, res1.getPriRequest().getPriBoolean().booleanValue());", assertionJavaCodeForResponse.get(8));
        assertEquals("assertEquals(false, res1.getPriRequest().isPribool());", assertionJavaCodeForResponse.get(9));
    }


    @Test
    public void testAccessFieldDtoWithNullCheck(){

        EndpointSchema endpoint = getOneEndpoint("accessFieldDtoCheck");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);


        PrivateFieldInRequestDto p1Instance = new PrivateFieldInRequestDto();

        p1.setValueBasedOnInstance(p1Instance);

        List<String> javaCodes = p1.newInstanceWithJava(0);

        assertEquals(11, javaCodes.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInRequestDto arg0 = null;", javaCodes.get(0));
        assertEquals("{", javaCodes.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodes.get(2));
        assertEquals(" arg0.pubField = null;", javaCodes.get(3));
        assertEquals(" arg0.setPriField(null);", javaCodes.get(4));
        assertEquals(" java.util.List<java.lang.String> arg0_stringList = null;", javaCodes.get(5));
        assertEquals(" arg0.setStringList(arg0_stringList);", javaCodes.get(6));
        assertEquals(" arg0.setPriEnum(null);", javaCodes.get(7));
        assertEquals(" arg0.setPriBoolean(null);", javaCodes.get(8));
        assertEquals(" arg0.setPribool(((boolean)(false)));", javaCodes.get(9));
        assertEquals("}", javaCodes.get(10));


        List<String> assertionJavaCode = p1.newAssertionWithJava(0, "res1", -1);
        assertEquals(6, assertionJavaCode.size());
        assertEquals("assertNull(res1.pubField);", assertionJavaCode.get(0));
        assertEquals("assertNull(res1.getPriField());", assertionJavaCode.get(1));
        assertEquals("assertNull(res1.getStringList());", assertionJavaCode.get(2));
        assertEquals("assertNull(res1.getPriEnum());", assertionJavaCode.get(3));
        assertEquals("assertNull(res1.getPriBoolean());", assertionJavaCode.get(4));
        assertEquals("assertEquals(false, res1.isPribool());", assertionJavaCode.get(5));

        NamedTypedValue res = endpoint.getResponse();
        PrivateFieldInResponseDto resInstance = new PrivateFieldInResponseDto(){{
            pubField = 42;
        }};

        res.setValueBasedOnInstance(resInstance);
        List<String> javaCodesForResponse = res.newInstanceWithJava(true, true, "tmp", 0);
        assertEquals(7, javaCodesForResponse.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInResponseDto tmp = null;", javaCodesForResponse.get(0));
        assertEquals("{", javaCodesForResponse.get(1));
        assertEquals(" tmp = new com.thrift.example.artificial.PrivateFieldInResponseDto();", javaCodesForResponse.get(2));
        assertEquals(" tmp.pubField = 42;", javaCodesForResponse.get(3));
        assertEquals(" com.thrift.example.artificial.PrivateFieldInRequestDto tmp_priRequest = null;", javaCodesForResponse.get(4));
        assertEquals(" tmp.setPriRequest(tmp_priRequest);", javaCodesForResponse.get(5));
        assertEquals("}", javaCodesForResponse.get(6));

        List<String> assertionJavaCodeForResponse = res.newAssertionWithJava(0, "res1", -1);
        assertEquals(2, assertionJavaCodeForResponse.size());
        assertEquals("assertEquals(42, res1.pubField);", assertionJavaCodeForResponse.get(0));
        assertEquals("assertNull(res1.getPriRequest());", assertionJavaCodeForResponse.get(1));

        PrivateFieldInResponseDto resInstance2 = new PrivateFieldInResponseDto(){{
            pubField = 42;
            setPriRequest(new PrivateFieldInRequestDto());
        }};

        res.setValueBasedOnInstance(resInstance2);

        javaCodesForResponse = res.newInstanceWithJava(true, true, "tmp", 0);
        assertEquals(17, javaCodesForResponse.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInResponseDto tmp = null;", javaCodesForResponse.get(0));
        assertEquals("{", javaCodesForResponse.get(1));
        assertEquals(" tmp = new com.thrift.example.artificial.PrivateFieldInResponseDto();", javaCodesForResponse.get(2));
        assertEquals(" tmp.pubField = 42;", javaCodesForResponse.get(3));
        assertEquals(" com.thrift.example.artificial.PrivateFieldInRequestDto tmp_priRequest = null;", javaCodesForResponse.get(4));
        assertEquals(" {", javaCodesForResponse.get(5));
        assertEquals("  tmp_priRequest = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodesForResponse.get(6));
        assertEquals("  tmp_priRequest.pubField = null;", javaCodesForResponse.get(7));
        assertEquals("  tmp_priRequest.setPriField(null);", javaCodesForResponse.get(8));
        assertEquals("  java.util.List<java.lang.String> tmp_priRequest_stringList = null;", javaCodesForResponse.get(9));
        assertEquals("  tmp_priRequest.setStringList(tmp_priRequest_stringList);", javaCodesForResponse.get(10));
        assertEquals("  tmp_priRequest.setPriEnum(null);", javaCodesForResponse.get(11));
        assertEquals("  tmp_priRequest.setPriBoolean(null);", javaCodesForResponse.get(12));
        assertEquals("  tmp_priRequest.setPribool(((boolean)(false)));", javaCodesForResponse.get(13));
        assertEquals(" }", javaCodesForResponse.get(14));
        assertEquals(" tmp.setPriRequest(tmp_priRequest);", javaCodesForResponse.get(15));
        assertEquals("}", javaCodesForResponse.get(16));

        assertionJavaCodeForResponse = res.newAssertionWithJava(0, "res1", -1);
        assertEquals(7, assertionJavaCodeForResponse.size());
        assertEquals("assertEquals(42, res1.pubField);", assertionJavaCodeForResponse.get(0));
        assertEquals("assertNull(res1.getPriRequest().pubField);", assertionJavaCodeForResponse.get(1));
        assertEquals("assertNull(res1.getPriRequest().getPriField());", assertionJavaCodeForResponse.get(2));
        assertEquals("assertNull(res1.getPriRequest().getStringList());", assertionJavaCodeForResponse.get(3));
        assertEquals("assertNull(res1.getPriRequest().getPriEnum());", assertionJavaCodeForResponse.get(4));
        assertEquals("assertNull(res1.getPriRequest().getPriBoolean());", assertionJavaCodeForResponse.get(5));
        assertEquals("assertEquals(false, res1.getPriRequest().isPribool());", assertionJavaCodeForResponse.get(6));
    }

    @Test
    public void testDateToString() throws ClassNotFoundException, ParseException {
        EndpointSchema endpoint = getOneEndpoint("dateToString");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof DateParam);

        //String stringDate = "2021-12-14 19:45:23.722 +0100";
        String stringDate = "2021-12-14 19:45:23";
        Date date = DateType.SIMPLE_DATE_FORMATTER.parse(stringDate);
        long time = date.getTime();

        p1.setValueBasedOnInstance(date);
        Object instance = p1.newInstance();
        assertTrue(instance instanceof Date);
        assertEquals(time, ((Date) instance).getTime());

        ParamDto dto = p1.getDto();
        assertEquals(6, dto.innerContent.size());
        assertEquals("2021", dto.innerContent.get(0).stringValue);
        assertEquals("12", dto.innerContent.get(1).stringValue);
        assertEquals("14", dto.innerContent.get(2).stringValue);
        assertEquals("19", dto.innerContent.get(3).stringValue);
        assertEquals("45", dto.innerContent.get(4).stringValue);
        assertEquals("23", dto.innerContent.get(5).stringValue);
//        assertEquals("722", dto.innerContent.get(6).jsonValue);
//        assertEquals("100", dto.innerContent.get(7).jsonValue);

        List<String> javacode = p1.newInstanceWithJava(0);
        assertEquals(5, javacode.size());
        assertEquals("java.util.Date arg0 = null;", javacode.get(0));
        assertEquals("{", javacode.get(1));
        assertEquals(" // Date is " + stringDate, javacode.get(2));
        assertEquals(" arg0 = new java.util.Date(" + time + "L);", javacode.get(3));
        assertEquals("}", javacode.get(4));

        List<String> assertions = p1.newAssertionWithJava(0, "res1", -1);
        assertEquals(1, assertions.size());
        assertTrue(assertions.get(0).contains("// runtime value is "));

    }

    @Test
    public void testSimplePrimitive() {

        EndpointSchema endpoint = getOneEndpoint("simplePrimitive");
        assertNotNull(endpoint.getResponse());
        assertEquals(8, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof IntParam);
        assertTrue(endpoint.getRequestParams().get(1) instanceof FloatParam);
        assertTrue(endpoint.getRequestParams().get(2) instanceof LongParam);
        assertTrue(endpoint.getRequestParams().get(3) instanceof DoubleParam);
        assertTrue(endpoint.getRequestParams().get(4) instanceof CharParam);
        assertTrue(endpoint.getRequestParams().get(5) instanceof ByteParam);
        assertTrue(endpoint.getRequestParams().get(6) instanceof BooleanParam);
        assertTrue(endpoint.getRequestParams().get(7) instanceof ShortParam);

    }

    @Test
    public void testSimplePrimitiveToFromDTO() throws ClassNotFoundException {

        EndpointSchema endpoint = getOneEndpoint("simplePrimitive");
        RPCActionDto dto = endpoint.getDto().copy();
        assertEquals(8, dto.requestParams.size());
        dto.requestParams.get(0).stringValue = "" + 42;
        dto.requestParams.get(1).stringValue = "" + 4.2f;
        dto.requestParams.get(2).stringValue = "" + 42L;
        dto.requestParams.get(3).stringValue = "" + 4.2;
        dto.requestParams.get(4).stringValue = "" + 'x';
        dto.requestParams.get(5).stringValue = "" + Byte.parseByte("42");
        dto.requestParams.get(6).stringValue = "" + false;
        dto.requestParams.get(7).stringValue = "" + Short.parseShort("42");
        endpoint.setValue(dto);
        assertEquals(42, endpoint.getRequestParams().get(0).newInstance());
        assertEquals(4.2f, endpoint.getRequestParams().get(1).newInstance());
        assertEquals(42L, endpoint.getRequestParams().get(2).newInstance());
        assertEquals(4.2, endpoint.getRequestParams().get(3).newInstance());
        assertEquals('x', endpoint.getRequestParams().get(4).newInstance());
        assertEquals(Byte.parseByte("42"), endpoint.getRequestParams().get(5).newInstance());
        assertEquals(false, endpoint.getRequestParams().get(6).newInstance());
        assertEquals(Short.parseShort("42"), endpoint.getRequestParams().get(7).newInstance());

    }

    @Test
    public void testSimpleWrapPrimitive() {

        EndpointSchema endpoint = getOneEndpoint("simpleWrapPrimitive");
        assertNotNull(endpoint.getResponse());
        assertEquals(8, endpoint.getRequestParams().size());
        assertTrue(endpoint.getRequestParams().get(0) instanceof IntParam);
        assertTrue(endpoint.getRequestParams().get(1) instanceof FloatParam);
        assertTrue(endpoint.getRequestParams().get(2) instanceof LongParam);
        assertTrue(endpoint.getRequestParams().get(3) instanceof DoubleParam);
        assertTrue(endpoint.getRequestParams().get(4) instanceof CharParam);
        assertTrue(endpoint.getRequestParams().get(5) instanceof ByteParam);
        assertTrue(endpoint.getRequestParams().get(6) instanceof BooleanParam);
        assertTrue(endpoint.getRequestParams().get(7) instanceof ShortParam);

    }

    @Test
    public void testSimpleWrapPrimitiveToFromDTO() throws ClassNotFoundException {

        EndpointSchema endpoint = getOneEndpoint("simpleWrapPrimitive");
        RPCActionDto dto = endpoint.getDto().copy();
        assertEquals(8, dto.requestParams.size());
        dto.requestParams.get(0).stringValue = "" + 42;
        dto.requestParams.get(1).stringValue = "" + 4.2f;
        dto.requestParams.get(2).stringValue = "" + 42L;
        dto.requestParams.get(3).stringValue = "" + 4.2;
        dto.requestParams.get(4).stringValue = "" + 'x';
        dto.requestParams.get(5).stringValue = "" + Byte.parseByte("42");
        dto.requestParams.get(6).stringValue = "" + false;
        dto.requestParams.get(7).stringValue = "" + Short.parseShort("42");
        endpoint.setValue(dto);
        assertEquals(42, endpoint.getRequestParams().get(0).newInstance());
        assertEquals(4.2f, endpoint.getRequestParams().get(1).newInstance());
        assertEquals(42L, endpoint.getRequestParams().get(2).newInstance());
        assertEquals(4.2, endpoint.getRequestParams().get(3).newInstance());
        assertEquals('x', endpoint.getRequestParams().get(4).newInstance());
        assertEquals(Byte.parseByte("42"), endpoint.getRequestParams().get(5).newInstance());
        assertEquals(false, endpoint.getRequestParams().get(6).newInstance());
        assertEquals(Short.parseShort("42"), endpoint.getRequestParams().get(7).newInstance());

    }

    @Test
    public void testArray() {

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

    }

    @Test
    public void testArrayToFromDto() throws ClassNotFoundException {

        EndpointSchema endpoint = getOneEndpoint("array");
        RPCActionDto dto = endpoint.getDto();
        assertEquals(1, dto.requestParams.size());
        ParamDto paramDto = dto.requestParams.get(0);
        assertEquals(RPCSupportedDataType.ARRAY, paramDto.type.type);
        assertNotNull(paramDto.type.example);
        ParamDto paramExampleDto = paramDto.type.example;
        assertEquals(RPCSupportedDataType.LIST, paramExampleDto.type.type);
        assertNotNull(paramExampleDto.type.example);
        ParamDto paramExampleExampleDto = paramExampleDto.type.example;
        assertEquals(RPCSupportedDataType.STRING, paramExampleExampleDto.type.type);

        List<ParamDto> strs = IntStream.range(0, 3).mapToObj(i -> {
            ParamDto p = paramExampleExampleDto.copy();
            p.stringValue = "str_" + i;
            return p;
        }).collect(Collectors.toList());

        ParamDto iList = paramExampleDto.copy();
        iList.innerContent = strs;

        paramDto.innerContent = Arrays.asList(iList);
        endpoint.setValue(dto);

    }


    @Test
    public void testArrayBoolean() {

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
    public void testList() {

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
    public void testMap() {

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
    public void testListAndMap() {

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
    public void testObjResponse() {

        EndpointSchema endpoint = getOneEndpoint("objResponse");
        assertEquals(0, endpoint.getRequestParams().size());

        assertNotNull(endpoint.getResponse());
        NamedTypedValue param = endpoint.getResponse();
        assertTrue(param instanceof ObjectParam);
        assertTrue(param.getType() instanceof ObjectType);

        List<NamedTypedValue> fs = ((ObjectType) param.getType()).getFields();
        assertEquals(7, fs.size());
        assertTrue(fs.get(0) instanceof StringParam);
        assertTrue(fs.get(1) instanceof IntParam);
        assertTrue(fs.get(2) instanceof DoubleParam);
        assertTrue(fs.get(3) instanceof ObjectParam);
        assertTrue(fs.get(4) instanceof ArrayParam);
        assertTrue(fs.get(5) instanceof ArrayParam);
        assertTrue(fs.get(6) instanceof LongParam);

        assertTrue(fs.get(3).getType() instanceof CycleObjectType);

    }

    @Test
    public void testObjCycleA() {

        EndpointSchema endpoint = getOneEndpoint("objCycleA");
        assertEquals(0, endpoint.getRequestParams().size());

        assertNotNull(endpoint.getResponse());
        NamedTypedValue param = endpoint.getResponse();
        assertTrue(param instanceof ObjectParam);
        assertTrue(param.getType() instanceof ObjectType);

        List<NamedTypedValue> fs = ((ObjectType) param.getType()).getFields();
        assertEquals(1, fs.size());
        assertTrue(fs.get(0) instanceof ObjectParam);
        assertEquals(1, ((ObjectParam) fs.get(0)).getType().getFields().size());
        assertTrue(((ObjectParam) fs.get(0)).getType().getFields().get(0).getType() instanceof CycleObjectType);

    }

    @Test
    public void testObjCycleB() {

        EndpointSchema endpoint = getOneEndpoint("objCycleB");
        assertEquals(0, endpoint.getRequestParams().size());

        assertNotNull(endpoint.getResponse());
        NamedTypedValue param = endpoint.getResponse();
        assertTrue(param instanceof ObjectParam);
        assertTrue(param.getType() instanceof ObjectType);

        List<NamedTypedValue> fs = ((ObjectType) param.getType()).getFields();
        assertEquals(1, fs.size());
        assertTrue(fs.get(0) instanceof ObjectParam);
        assertEquals(1, ((ObjectParam) fs.get(0)).getType().getFields().size());
        assertTrue(((ObjectParam) fs.get(0)).getType().getFields().get(0).getType() instanceof CycleObjectType);

    }

}
