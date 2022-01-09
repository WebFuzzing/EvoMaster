package org.evomaster.client.java.controller.problem.rpc;

import com.thrift.example.artificial.AuthLoginDto;
import com.thrift.example.artificial.RPCInterfaceExample;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
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
        return 15;
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
                    keyValues = new keyValuesDto() {{
                        key = "value";
                        values = Arrays.asList("0.42", "42.42", "100.42");
                    }};
                }}
        );
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
        assertTrue(p1 instanceof ObjectParam);
        assertTrue(p1.isNullable());
        assertEquals(7, ((ObjectParam) p1).getType().getFields().size());
        for (NamedTypedValue f : ((ObjectParam) p1).getType().getFields()) {
            if (f.getName().equals("list")) {
                assertTrue(f instanceof ListParam);
                assertFalse(f.isNullable());
                assertEquals(1, ((ListParam) f).getMinSize());
            } else if (f.getName().equals("listSize")) {
                assertTrue(f instanceof ListParam);
                assertEquals(1, ((ListParam) f).getMinSize());
                assertEquals(10, ((ListParam) f).getMaxSize());
            } else if (f.getName().equals("intWithMinMax")) {
                assertTrue(f instanceof IntParam);
                assertEquals(0, ((IntParam) f).getMin().intValue());
                assertEquals(100, ((IntParam) f).getMax().intValue());
            } else if (f.getName().equals("longWithMinMax")) {
                assertTrue(f instanceof LongParam);
                assertEquals(-100L, ((LongParam) f).getMin());
                assertEquals(1000L, ((LongParam) f).getMax());
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
            } else
                fail("do not handle param " + f.getName());
        }


        NamedTypedValue p2 = endpoint.getRequestParams().get(1);
        assertTrue(p2 instanceof StringParam);
        assertFalse(p2.isNullable());
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
        assertEquals(6, fs.size());
        assertTrue(fs.get(0) instanceof StringParam);
        assertTrue(fs.get(1) instanceof IntParam);
        assertTrue(fs.get(2) instanceof DoubleParam);
        assertTrue(fs.get(3) instanceof ObjectParam);
        assertTrue(fs.get(4) instanceof ArrayParam);
        assertTrue(fs.get(5) instanceof ArrayParam);

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
