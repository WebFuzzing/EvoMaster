package org.evomaster.client.java.controller.problem.rpc;

import com.thrift.example.artificial.*;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.auth.JsonAuthRPCEndpointDto;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.problem.rpc.schema.types.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
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
        return 44;
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
    public void testNumericString(){
        EndpointSchema endpoint = getOneEndpoint("numericString");
        assertNotNull(endpoint.getResponse());
        assertNotNull(endpoint.getRequestParams());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);

        assertEquals(4, ((ObjectParam)p1).getType().getFields().size());
        for (NamedTypedValue p : ((ObjectParam)p1).getType().getFields()){
            assertTrue(p instanceof StringParam);
            StringParam ns = (StringParam) p;
            if (p.getName().equals("longValue")){
                assertTrue(p.isNullable());
                assertFalse(ns.getMaxInclusive());
                assertTrue(ns.getMinInclusive());
                assertEquals(Long.MAX_VALUE, ns.getMax().longValue());
                assertNull(ns.getMin());
            }else if (p.getName().equals("intValue")){
                assertFalse(p.isNullable());
                assertEquals(Integer.MAX_VALUE, ns.getMax().intValue());
                assertFalse(ns.getMaxInclusive());
                assertTrue(ns.getMinInclusive());
            }else if (p.getName().equals("bigIntegerValue")){
                assertTrue(p.isNullable());
                assertEquals(Long.MAX_VALUE, ns.getMax().longValue());
                assertFalse(ns.getMaxInclusive());
                assertTrue(ns.getMinInclusive());
            }else if (p.getName().equals("bigDecimalValue")){
                assertTrue(p.isNullable());
                assertEquals(Double.MAX_VALUE, ns.getMax().doubleValue());
                assertFalse(ns.getMaxInclusive());
                assertEquals(15, ns.getPrecision());
                assertEquals(5, ns.getScale());
                assertFalse(ns.getMinInclusive());
                assertEquals(0.0, ns.getMin().doubleValue());
            }
        }
    }

    @Test
    public void testImmutableObj(){

        EndpointSchema endpoint = getOneEndpoint("immutableObj");
        assertNotNull(endpoint.getResponse());
        assertNotNull(endpoint.getRequestParams());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);

        assertEquals(3, ((ObjectParam)p1).getType().getFields().size());
        for (NamedTypedValue p : ((ObjectParam)p1).getType().getFields()){
            assertFalse(p.isMutable());
            if (p.getName().equals("nullLong")){
                assertTrue(p.isNullable());
                assertNull(p.getDefaultValue());
            }else if (p.getName().equals("pbool")){
                assertFalse(p.isNullable());
                assertNotNull(p.getDefaultValue());
                assertTrue(p.getDefaultValue() instanceof BooleanParam);
                assertFalse(((BooleanParam)p.getDefaultValue()).getValue());
            }else if (p.getName().equals("wbool")){
                assertTrue(p.isNullable());
                assertNotNull(p.getDefaultValue());
                assertTrue(p.getDefaultValue() instanceof BooleanParam);
                assertTrue(((BooleanParam)p.getDefaultValue()).getValue());
            }
        }
    }



    @Test
    public void testbigNumber(){
        EndpointSchema endpoint = getOneEndpoint("bigNumber");
        assertNotNull(endpoint.getResponse());
        assertNotNull(endpoint.getRequestParams());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);

        assertEquals(8, ((ObjectParam)p1).getType().getFields().size());
        for (NamedTypedValue p : ((ObjectParam)p1).getType().getFields()){

            assertTrue(p instanceof NumericConstraintBase);
            if (p.getName().equals("bdPositiveFloat")){
                assertEquals(4, ((NumericConstraintBase)p).getPrecision());
                assertEquals(2, ((NumericConstraintBase)p).getScale());
                assertEquals("42.42", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("0", ((NumericConstraintBase)p).getMin().toString());
                assertFalse(((NumericConstraintBase) p).getMinInclusive());
                assertTrue(((NumericConstraintBase) p).getMaxInclusive());

            }else if (p.getName().equals("bdPositiveOrZeroFloat")){
                assertEquals(4, ((NumericConstraintBase)p).getPrecision());
                assertEquals(2, ((NumericConstraintBase)p).getScale());
                assertEquals("42.42", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("0", ((NumericConstraintBase)p).getMin().toString());
                assertTrue(((NumericConstraintBase) p).getMinInclusive());
                assertTrue(((NumericConstraintBase) p).getMaxInclusive());

            } else if (p.getName().equals("bdNegativeFloat")){
                assertEquals(4, ((NumericConstraintBase)p).getPrecision());
                assertEquals(2, ((NumericConstraintBase)p).getScale());
                assertEquals("0", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("-42.42", ((NumericConstraintBase)p).getMin().toString());
                assertTrue(((NumericConstraintBase) p).getMinInclusive());
                assertFalse(((NumericConstraintBase) p).getMaxInclusive());

            } else if (p.getName().equals("bdNegativeOrZeroFloat")){
                assertEquals(4, ((NumericConstraintBase)p).getPrecision());
                assertEquals(2, ((NumericConstraintBase)p).getScale());
                assertEquals("0", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("-42.42", ((NumericConstraintBase)p).getMin().toString());
                assertTrue(((NumericConstraintBase) p).getMinInclusive());
                assertTrue(((NumericConstraintBase) p).getMaxInclusive());

            } else if (p.getName().equals("biPositive")){
                assertEquals(2, ((NumericConstraintBase)p).getPrecision());
                assertEquals(0, ((NumericConstraintBase)p).getScale());
                assertEquals("42", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("0", ((NumericConstraintBase)p).getMin().toString());
                assertFalse(((NumericConstraintBase) p).getMinInclusive());
                assertTrue(((NumericConstraintBase) p).getMaxInclusive());

            }else if (p.getName().equals("biPositiveOrZero")){
                assertEquals(2, ((NumericConstraintBase)p).getPrecision());
                assertEquals(0, ((NumericConstraintBase)p).getScale());
                assertEquals("42", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("0", ((NumericConstraintBase)p).getMin().toString());
                assertTrue(((NumericConstraintBase) p).getMinInclusive());
                assertTrue(((NumericConstraintBase) p).getMaxInclusive());

            } else if (p.getName().equals("biNegative")){
                assertEquals(2, ((NumericConstraintBase)p).getPrecision());
                assertEquals(0, ((NumericConstraintBase)p).getScale());
                assertEquals("0", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("-42", ((NumericConstraintBase)p).getMin().toString());
                assertTrue(((NumericConstraintBase) p).getMinInclusive());
                assertFalse(((NumericConstraintBase) p).getMaxInclusive());

            } else if (p.getName().equals("biNegativeOrZero")){
                assertEquals(2, ((NumericConstraintBase)p).getPrecision());
                assertEquals(0, ((NumericConstraintBase)p).getScale());
                assertEquals("0", ((NumericConstraintBase)p).getMax().toString());
                assertEquals("-42", ((NumericConstraintBase)p).getMin().toString());
                assertTrue(((NumericConstraintBase) p).getMinInclusive());
                assertTrue(((NumericConstraintBase) p).getMaxInclusive());

            }
        }

        BigNumberObj bigNumberObj = new BigNumberObj(){{
           // bigdecimal
           setBdPositiveFloat(new BigDecimal("10.12"));
           setBdPositiveOrZeroFloat(new BigDecimal("0.00"));
           setBdNegativeFloat(new BigDecimal("-10.12"));
           setBdNegativeOrZeroFloat(new BigDecimal("-2.16"));

           // biginteger
           setBiPositive(BigInteger.TEN);
           setBiPositiveOrZero(BigInteger.ZERO);
           setBiNegative(BigInteger.valueOf(-10));
           setBiNegativeOrZero(BigInteger.valueOf(-2));
        }};

        p1.setValueBasedOnInstance(bigNumberObj);
        ParamDto dto = p1.getDto();
        assertNotNull(dto.stringValue);
        assertEquals(8, dto.innerContent.size());
        assertEquals("10.12", dto.innerContent.get(0).stringValue);
        assertEquals("-10.12", dto.innerContent.get(1).stringValue);
        assertEquals("0.00", dto.innerContent.get(2).stringValue);
        assertEquals("-2.16", dto.innerContent.get(3).stringValue);
        assertEquals("10", dto.innerContent.get(4).stringValue);
        assertEquals("0", dto.innerContent.get(5).stringValue);
        assertEquals("-10", dto.innerContent.get(6).stringValue);
        assertEquals("-2", dto.innerContent.get(7).stringValue);

        List<String> testScript = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals("com.thrift.example.artificial.BigNumberObj arg0 = null;",testScript.get(0));
        assertEquals("{",testScript.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.BigNumberObj();",testScript.get(2));
        assertEquals(" java.math.BigDecimal arg0_bdPositiveFloat = null;",testScript.get(3));
        assertEquals(" {",testScript.get(4));
        assertEquals("  java.math.MathContext arg0_bdPositiveFloat_mc = new java.math.MathContext(4);",testScript.get(5));
        assertEquals("  arg0_bdPositiveFloat = new java.math.BigDecimal(\"10.12\", arg0_bdPositiveFloat_mc);",testScript.get(6));
        assertEquals("  arg0_bdPositiveFloat.setScale(2, java.math.RoundingMode.HALF_UP);",testScript.get(7));
        assertEquals(" }",testScript.get(8));
        assertEquals(" arg0.setBdPositiveFloat(arg0_bdPositiveFloat);",testScript.get(9));
        assertEquals(" java.math.BigDecimal arg0_bdNegativeFloat = null;",testScript.get(10));
        assertEquals(" {",testScript.get(11));
        assertEquals("  java.math.MathContext arg0_bdNegativeFloat_mc = new java.math.MathContext(4);",testScript.get(12));
        assertEquals("  arg0_bdNegativeFloat = new java.math.BigDecimal(\"-10.12\", arg0_bdNegativeFloat_mc);",testScript.get(13));
        assertEquals("  arg0_bdNegativeFloat.setScale(2, java.math.RoundingMode.HALF_UP);",testScript.get(14));
        assertEquals(" }",testScript.get(15));
        assertEquals(" arg0.setBdNegativeFloat(arg0_bdNegativeFloat);",testScript.get(16));
        assertEquals(" java.math.BigDecimal arg0_bdPositiveOrZeroFloat = null;",testScript.get(17));
        assertEquals(" {",testScript.get(18));
        assertEquals("  java.math.MathContext arg0_bdPositiveOrZeroFloat_mc = new java.math.MathContext(4);",testScript.get(19));
        assertEquals("  arg0_bdPositiveOrZeroFloat = new java.math.BigDecimal(\"0.00\", arg0_bdPositiveOrZeroFloat_mc);",testScript.get(20));
        assertEquals("  arg0_bdPositiveOrZeroFloat.setScale(2, java.math.RoundingMode.HALF_UP);",testScript.get(21));
        assertEquals(" }",testScript.get(22));
        assertEquals(" arg0.setBdPositiveOrZeroFloat(arg0_bdPositiveOrZeroFloat);",testScript.get(23));
        assertEquals(" java.math.BigDecimal arg0_bdNegativeOrZeroFloat = null;",testScript.get(24));
        assertEquals(" {",testScript.get(25));
        assertEquals("  java.math.MathContext arg0_bdNegativeOrZeroFloat_mc = new java.math.MathContext(4);",testScript.get(26));
        assertEquals("  arg0_bdNegativeOrZeroFloat = new java.math.BigDecimal(\"-2.16\", arg0_bdNegativeOrZeroFloat_mc);",testScript.get(27));
        assertEquals("  arg0_bdNegativeOrZeroFloat.setScale(2, java.math.RoundingMode.HALF_UP);",testScript.get(28));
        assertEquals(" }",testScript.get(29));
        assertEquals(" arg0.setBdNegativeOrZeroFloat(arg0_bdNegativeOrZeroFloat);",testScript.get(30));
        assertEquals(" java.math.BigInteger arg0_biPositive = null;",testScript.get(31));
        assertEquals(" {",testScript.get(32));
        assertEquals("  arg0_biPositive = new java.math.BigInteger(\"10\");",testScript.get(33));
        assertEquals(" }",testScript.get(34));
        assertEquals(" arg0.setBiPositive(arg0_biPositive);",testScript.get(35));
        assertEquals(" java.math.BigInteger arg0_biPositiveOrZero = null;",testScript.get(36));
        assertEquals(" {",testScript.get(37));
        assertEquals("  arg0_biPositiveOrZero = new java.math.BigInteger(\"0\");",testScript.get(38));
        assertEquals(" }",testScript.get(39));
        assertEquals(" arg0.setBiPositiveOrZero(arg0_biPositiveOrZero);",testScript.get(40));
        assertEquals(" java.math.BigInteger arg0_biNegative = null;",testScript.get(41));
        assertEquals(" {",testScript.get(42));
        assertEquals("  arg0_biNegative = new java.math.BigInteger(\"-10\");",testScript.get(43));
        assertEquals(" }",testScript.get(44));
        assertEquals(" arg0.setBiNegative(arg0_biNegative);",testScript.get(45));
        assertEquals(" java.math.BigInteger arg0_biNegativeOrZero = null;",testScript.get(46));
        assertEquals(" {",testScript.get(47));
        assertEquals("  arg0_biNegativeOrZero = new java.math.BigInteger(\"-2\");",testScript.get(48));
        assertEquals(" }",testScript.get(49));
        assertEquals(" arg0.setBiNegativeOrZero(arg0_biNegativeOrZero);",testScript.get(50));
        assertEquals("}",testScript.get(51));

        List<String> assertionScript = p1.newAssertionWithJavaOrKotlin("arg0", 0, true);
        assertEquals("assertEquals(\"10.12\", arg0.getBdPositiveFloat().toString());", assertionScript.get(0));
        assertEquals("assertEquals(\"-10.12\", arg0.getBdNegativeFloat().toString());", assertionScript.get(1));
        assertEquals("assertEquals(\"0.00\", arg0.getBdPositiveOrZeroFloat().toString());", assertionScript.get(2));
        assertEquals("assertEquals(\"-2.16\", arg0.getBdNegativeOrZeroFloat().toString());", assertionScript.get(3));
        assertEquals("assertEquals(\"10\", arg0.getBiPositive().toString());", assertionScript.get(4));
        assertEquals("assertEquals(\"0\", arg0.getBiPositiveOrZero().toString());", assertionScript.get(5));
        assertEquals("assertEquals(\"-10\", arg0.getBiNegative().toString());", assertionScript.get(6));
        assertEquals("assertEquals(\"-2\", arg0.getBiNegativeOrZero().toString());", assertionScript.get(7));
    }

    @Test
    public void testEnumWithConstructor(){
        EndpointSchema endpoint = getOneEndpoint("handleEnumWithConstructor");
        assertNotNull(endpoint.getResponse());
        assertNotNull(endpoint.getRequestParams());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);

        ObjectEnum objectEnum = new ObjectEnum(){{
            enumWithConstructor = EnumWithConstructor.FIRST;
        }};

        p1.setValueBasedOnInstance(objectEnum);
        List<String> testScript = p1.newInstanceWithJavaOrKotlin(0, true, true);

        assertEquals(5, testScript.size());
        assertEquals("com.thrift.example.artificial.ObjectEnum arg0 = null;", testScript.get(0));
        assertEquals("{", testScript.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.ObjectEnum();", testScript.get(2));
        assertEquals(" arg0.enumWithConstructor = com.thrift.example.artificial.EnumWithConstructor.FIRST;", testScript.get(3));
        assertEquals("}", testScript.get(4));
    }

    @Test
    public void testNestedGeneric() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("handleNestedGenericString");
        assertNotNull(endpoint.getResponse());
        assertNotNull(endpoint.getRequestParams());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof ObjectParam);
        assertEquals(4, ((ObjectParam)p1).getType().getFields().size());
        assertNull(((ObjectParam)p1).getValue());
        Object p1Instance = p1.newInstance();
        assertNull(p1Instance);
        List<String> testScript = p1.newInstanceWithJavaOrKotlin(0, true, true);
        ParamDto dto  = p1.getDto();
        dto.innerContent = null;
        p1.setValueBasedOnDto(dto);
        List<String> testScriptWithDto =  p1.newInstanceWithJavaOrKotlin(0, true, true);
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
        List<String> javaCode  = p1.newInstanceWithJavaOrKotlin(0, true, true);
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
                assertEquals(-100L, ((LongParam) f).getMin().longValue());
                assertEquals(1000L, ((LongParam) f).getMax().longValue());
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
                assertEquals(1L, ((LongParam) f).getMin().longValue());
                assertEquals(10L, ((LongParam) f).getMax().longValue());
                assertFalse(f.isNullable());
            }else if(f.getName().equals("longWithInclusiveFDecimalMainMax")){
                assertTrue(f instanceof LongParam);
                assertEquals(1L, ((LongParam) f).getMin().longValue());
                assertFalse(((LongParam) f).getMinInclusive());
                assertEquals(10L, ((LongParam) f).getMax().longValue());
                assertFalse(((LongParam) f).getMaxInclusive());
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
        assertEquals(13, fs.size());

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
            } else if (f.getName().equals("pribyte")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPribyte", f.accessibleSchema.setterMethodName);
                assertEquals("getPribyte", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priBByte")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriBByte", f.accessibleSchema.setterMethodName);
                assertEquals("getPriBByte", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priCharacter")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriCharacter", f.accessibleSchema.setterMethodName);
                assertEquals("getPriCharacter", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priChar")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriChar", f.accessibleSchema.setterMethodName);
                assertEquals("getPriChar", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priSShort")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriSShort", f.accessibleSchema.setterMethodName);
                assertEquals("getPriSShort", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priShot")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriShot", f.accessibleSchema.setterMethodName);
                assertEquals("getPriShot", f.accessibleSchema.getterMethodName);
            } else if (f.getName().equals("priMap")){
                assertFalse(f.accessibleSchema.isAccessible);
                assertEquals("setPriMap", f.accessibleSchema.setterMethodName);
                assertEquals("getPriMap", f.accessibleSchema.getterMethodName);
            }
        }

        PrivateFieldInRequestDto p1Instance = new PrivateFieldInRequestDto(){{
            pubField = "foo";
            setPriField("bar");
            setStringList(Arrays.asList("1","2","3"));
            setPriEnum(EnumKind.ONE);
            setPriBoolean(true);
            setPriBByte((byte) 15);
            setPribyte((byte) 5);
            setPriChar('0');
            setPriCharacter('a');
            setPriShort((short) 2);
            setPriSShort((short) 42);
            setPriMap(new HashMap<String, String>(){{
                put("foo","foo");
                put("bar", "bar");
            }});
        }};

        p1.setValueBasedOnInstance(p1Instance);

        List<String> javaCodes = p1.newInstanceWithJavaOrKotlin(0, true, true);

        assertEquals(37, javaCodes.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInRequestDto arg0 = null;", javaCodes.get(0));
        assertEquals("{", javaCodes.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodes.get(2));
        assertEquals(" arg0.setPubField(\"foo\");", javaCodes.get(3));
        assertEquals(" arg0.setPriField(\"bar\");", javaCodes.get(4));
        assertEquals(" java.util.List<String> arg0_stringList = null;", javaCodes.get(5));
        assertEquals(" {", javaCodes.get(6));
        assertEquals("  arg0_stringList = new java.util.ArrayList<>();", javaCodes.get(7));
        assertEquals("  String arg0_stringList_e_0 = \"1\";", javaCodes.get(8));
        assertEquals("  arg0_stringList.add(arg0_stringList_e_0);", javaCodes.get(9));
        assertEquals("  String arg0_stringList_e_1 = \"2\";", javaCodes.get(10));
        assertEquals("  arg0_stringList.add(arg0_stringList_e_1);", javaCodes.get(11));
        assertEquals("  String arg0_stringList_e_2 = \"3\";", javaCodes.get(12));
        assertEquals("  arg0_stringList.add(arg0_stringList_e_2);", javaCodes.get(13));
        assertEquals(" }", javaCodes.get(14));
        assertEquals(" arg0.setStringList(arg0_stringList);", javaCodes.get(15));
        assertEquals(" arg0.setPriEnum(((com.thrift.example.artificial.EnumKind)(com.thrift.example.artificial.EnumKind.ONE)));", javaCodes.get(16));
        assertEquals(" arg0.setPriBoolean(true);", javaCodes.get(17));
        assertEquals(" arg0.setPribool(false);", javaCodes.get(18));
        assertEquals(" arg0.setPriBByte(((byte)(15)));", javaCodes.get(19));
        assertEquals(" arg0.setPribyte(((byte)(5)));", javaCodes.get(20));
        assertEquals(" arg0.setPriCharacter('\\u0061');", javaCodes.get(21));
        assertEquals(" arg0.setPriChar('\\u0030');", javaCodes.get(22));
        assertEquals(" arg0.setPriShort(((short)(2)));", javaCodes.get(23));
        assertEquals(" arg0.setPriSShort(((short)(42)));", javaCodes.get(24));
        assertEquals(" java.util.Map<String,String> arg0_priMap = null;", javaCodes.get(25));
        assertEquals(" {", javaCodes.get(26));
        assertEquals("  arg0_priMap = new java.util.HashMap<>();", javaCodes.get(27));
        assertEquals("  String arg0_priMap_key_0 = \"bar\";", javaCodes.get(28));
        assertEquals("  String arg0_priMap_value_0 = \"bar\";", javaCodes.get(29));
        assertEquals("  arg0_priMap.put(arg0_priMap_key_0,arg0_priMap_value_0);", javaCodes.get(30));
        assertEquals("  String arg0_priMap_key_1 = \"foo\";", javaCodes.get(31));
        assertEquals("  String arg0_priMap_value_1 = \"foo\";", javaCodes.get(32));
        assertEquals("  arg0_priMap.put(arg0_priMap_key_1,arg0_priMap_value_1);", javaCodes.get(33));
        assertEquals(" }", javaCodes.get(34));
        assertEquals(" arg0.setPriMap(arg0_priMap);", javaCodes.get(35));
        assertEquals("}", javaCodes.get(36));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);

        assertEquals(18, assertionJavaCode.size());
        assertEquals("assertEquals(\"foo\", res1.pubField);", assertionJavaCode.get(0));
        assertEquals("assertEquals(\"bar\", res1.getPriField());", assertionJavaCode.get(1));
        assertEquals("assertEquals(3, res1.getStringList().size());", assertionJavaCode.get(2));
        assertEquals("assertEquals(\"1\", res1.getStringList().get(0));", assertionJavaCode.get(3));
        assertEquals("assertEquals(\"2\", res1.getStringList().get(1));", assertionJavaCode.get(4));
        assertEquals("assertEquals(\"3\", res1.getStringList().get(2));", assertionJavaCode.get(5));
        assertEquals("//assertEquals(com.thrift.example.artificial.EnumKind.ONE, res1.getPriEnum());", assertionJavaCode.get(6));
        assertEquals("assertEquals(true, res1.getPriBoolean().booleanValue());", assertionJavaCode.get(7));
        assertEquals("assertEquals(false, res1.isPribool());", assertionJavaCode.get(8));
        assertEquals("assertEquals(15, res1.getPriBByte().byteValue());", assertionJavaCode.get(9));
        assertEquals("assertEquals(5, res1.getPribyte());", assertionJavaCode.get(10));
        assertEquals("assertEquals('\\u0061', res1.getPriCharacter().charValue());", assertionJavaCode.get(11));
        assertEquals("assertEquals('\\u0030', res1.getPriChar());", assertionJavaCode.get(12));
        assertEquals("assertEquals(2, res1.getPriShort().shortValue());", assertionJavaCode.get(13));
        assertEquals("assertEquals(42, res1.getPriSShort().shortValue());", assertionJavaCode.get(14));
        assertEquals("assertEquals(2, res1.getPriMap().size());", assertionJavaCode.get(15));
        assertEquals("assertEquals(\"bar\", res1.getPriMap().get(\"bar\"));", assertionJavaCode.get(16));
        assertEquals("assertEquals(\"foo\", res1.getPriMap().get(\"foo\"));", assertionJavaCode.get(17));

        NamedTypedValue res = endpoint.getResponse();
        PrivateFieldInResponseDto resInstance = new PrivateFieldInResponseDto(){{
            pubField = 42;
            setPriRequest(p1Instance);
        }};

        res.setValueBasedOnInstance(resInstance);
        List<String> javaCodesForResponse = res.newInstanceWithJavaOrKotlin(true, true, "tmp", 0, true, true);

        assertEquals(43, javaCodesForResponse.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInResponseDto tmp = null;", javaCodesForResponse.get(0));
        assertEquals("{", javaCodesForResponse.get(1));
        assertEquals(" tmp = new com.thrift.example.artificial.PrivateFieldInResponseDto();", javaCodesForResponse.get(2));
        assertEquals(" tmp.pubField = 42;", javaCodesForResponse.get(3));
        assertEquals(" com.thrift.example.artificial.PrivateFieldInRequestDto tmp_priRequest = null;", javaCodesForResponse.get(4));
        assertEquals(" {", javaCodesForResponse.get(5));
        assertEquals("  tmp_priRequest = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodesForResponse.get(6));
        assertEquals("  tmp_priRequest.setPubField(\"foo\");", javaCodesForResponse.get(7));
        assertEquals("  tmp_priRequest.setPriField(\"bar\");", javaCodesForResponse.get(8));
        assertEquals("  java.util.List<String> tmp_priRequest_stringList = null;", javaCodesForResponse.get(9));
        assertEquals("  {", javaCodesForResponse.get(10));
        assertEquals("   tmp_priRequest_stringList = new java.util.ArrayList<>();", javaCodesForResponse.get(11));
        assertEquals("   String tmp_priRequest_stringList_e_0 = \"1\";", javaCodesForResponse.get(12));
        assertEquals("   tmp_priRequest_stringList.add(tmp_priRequest_stringList_e_0);", javaCodesForResponse.get(13));
        assertEquals("   String tmp_priRequest_stringList_e_1 = \"2\";", javaCodesForResponse.get(14));
        assertEquals("   tmp_priRequest_stringList.add(tmp_priRequest_stringList_e_1);", javaCodesForResponse.get(15));
        assertEquals("   String tmp_priRequest_stringList_e_2 = \"3\";", javaCodesForResponse.get(16));
        assertEquals("   tmp_priRequest_stringList.add(tmp_priRequest_stringList_e_2);", javaCodesForResponse.get(17));
        assertEquals("  }", javaCodesForResponse.get(18));
        assertEquals("  tmp_priRequest.setStringList(tmp_priRequest_stringList);", javaCodesForResponse.get(19));
        assertEquals("  tmp_priRequest.setPriEnum(((com.thrift.example.artificial.EnumKind)(com.thrift.example.artificial.EnumKind.ONE)));", javaCodesForResponse.get(20));
        assertEquals("  tmp_priRequest.setPriBoolean(true);", javaCodesForResponse.get(21));
        assertEquals("  tmp_priRequest.setPribool(false);", javaCodesForResponse.get(22));
        assertEquals("  tmp_priRequest.setPriBByte(((byte)(15)));", javaCodesForResponse.get(23));
        assertEquals("  tmp_priRequest.setPribyte(((byte)(5)));", javaCodesForResponse.get(24));
        assertEquals("  tmp_priRequest.setPriCharacter('\\u0061');", javaCodesForResponse.get(25));
        assertEquals("  tmp_priRequest.setPriChar('\\u0030');", javaCodesForResponse.get(26));
        assertEquals("  tmp_priRequest.setPriShort(((short)(2)));", javaCodesForResponse.get(27));
        assertEquals("  tmp_priRequest.setPriSShort(((short)(42)));", javaCodesForResponse.get(28));
        assertEquals("  java.util.Map<String,String> tmp_priRequest_priMap = null;", javaCodesForResponse.get(29));
        assertEquals("  {", javaCodesForResponse.get(30));
        assertEquals("   tmp_priRequest_priMap = new java.util.HashMap<>();", javaCodesForResponse.get(31));
        assertEquals("   String tmp_priRequest_priMap_key_0 = \"bar\";", javaCodesForResponse.get(32));
        assertEquals("   String tmp_priRequest_priMap_value_0 = \"bar\";", javaCodesForResponse.get(33));
        assertEquals("   tmp_priRequest_priMap.put(tmp_priRequest_priMap_key_0,tmp_priRequest_priMap_value_0);", javaCodesForResponse.get(34));
        assertEquals("   String tmp_priRequest_priMap_key_1 = \"foo\";", javaCodesForResponse.get(35));
        assertEquals("   String tmp_priRequest_priMap_value_1 = \"foo\";", javaCodesForResponse.get(36));
        assertEquals("   tmp_priRequest_priMap.put(tmp_priRequest_priMap_key_1,tmp_priRequest_priMap_value_1);", javaCodesForResponse.get(37));
        assertEquals("  }", javaCodesForResponse.get(38));
        assertEquals("  tmp_priRequest.setPriMap(tmp_priRequest_priMap);", javaCodesForResponse.get(39));
        assertEquals(" }", javaCodesForResponse.get(40));
        assertEquals(" tmp.setPriRequest(tmp_priRequest);", javaCodesForResponse.get(41));
        assertEquals("}", javaCodesForResponse.get(42));

        List<String> assertionJavaCodeForResponse = res.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(19, assertionJavaCodeForResponse.size());
        assertEquals("assertEquals(42, res1.pubField);", assertionJavaCodeForResponse.get(0));
        assertEquals("assertEquals(\"foo\", res1.getPriRequest().pubField);", assertionJavaCodeForResponse.get(1));
        assertEquals("assertEquals(\"bar\", res1.getPriRequest().getPriField());", assertionJavaCodeForResponse.get(2));
        assertEquals("assertEquals(3, res1.getPriRequest().getStringList().size());", assertionJavaCodeForResponse.get(3));
        assertEquals("assertEquals(\"1\", res1.getPriRequest().getStringList().get(0));", assertionJavaCodeForResponse.get(4));
        assertEquals("assertEquals(\"2\", res1.getPriRequest().getStringList().get(1));", assertionJavaCodeForResponse.get(5));
        assertEquals("assertEquals(\"3\", res1.getPriRequest().getStringList().get(2));", assertionJavaCodeForResponse.get(6));
        assertEquals("//assertEquals(com.thrift.example.artificial.EnumKind.ONE, res1.getPriRequest().getPriEnum());", assertionJavaCodeForResponse.get(7));
        assertEquals("assertEquals(true, res1.getPriRequest().getPriBoolean().booleanValue());", assertionJavaCodeForResponse.get(8));
        assertEquals("assertEquals(false, res1.getPriRequest().isPribool());", assertionJavaCodeForResponse.get(9));
        assertEquals("assertEquals(15, res1.getPriRequest().getPriBByte().byteValue());", assertionJavaCodeForResponse.get(10));
        assertEquals("assertEquals(5, res1.getPriRequest().getPribyte());", assertionJavaCodeForResponse.get(11));
        assertEquals("assertEquals('\\u0061', res1.getPriRequest().getPriCharacter().charValue());", assertionJavaCodeForResponse.get(12));
        assertEquals("assertEquals('\\u0030', res1.getPriRequest().getPriChar());", assertionJavaCodeForResponse.get(13));
        assertEquals("assertEquals(2, res1.getPriRequest().getPriShort().shortValue());", assertionJavaCodeForResponse.get(14));
        assertEquals("assertEquals(42, res1.getPriRequest().getPriSShort().shortValue());", assertionJavaCodeForResponse.get(15));
        assertEquals("assertEquals(2, res1.getPriRequest().getPriMap().size());", assertionJavaCodeForResponse.get(16));
        assertEquals("assertEquals(\"bar\", res1.getPriRequest().getPriMap().get(\"bar\"));", assertionJavaCodeForResponse.get(17));
        assertEquals("assertEquals(\"foo\", res1.getPriRequest().getPriMap().get(\"foo\"));", assertionJavaCodeForResponse.get(18));
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

        List<String> javaCodes = p1.newInstanceWithJavaOrKotlin(0, true, true);

        assertEquals(19, javaCodes.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInRequestDto arg0 = null;", javaCodes.get(0));
        assertEquals("{", javaCodes.get(1));
        assertEquals(" arg0 = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodes.get(2));
        assertEquals(" arg0.setPubField(null);", javaCodes.get(3));
        assertEquals(" arg0.setPriField(null);", javaCodes.get(4));
        assertEquals(" java.util.List<String> arg0_stringList = null;", javaCodes.get(5));
        assertEquals(" arg0.setStringList(arg0_stringList);", javaCodes.get(6));
        assertEquals(" arg0.setPriEnum(null);", javaCodes.get(7));
        assertEquals(" arg0.setPriBoolean(null);", javaCodes.get(8));
        assertEquals(" arg0.setPribool(false);", javaCodes.get(9));
        assertEquals(" arg0.setPriBByte(null);", javaCodes.get(10));
        assertEquals(" arg0.setPribyte(((byte)(0)));", javaCodes.get(11));
        assertEquals(" arg0.setPriCharacter(null);", javaCodes.get(12));
        assertEquals(" arg0.setPriChar('\\u0000');", javaCodes.get(13));
        assertEquals(" arg0.setPriShort(((short)(0)));", javaCodes.get(14));
        assertEquals(" arg0.setPriSShort(null);", javaCodes.get(15));
        assertEquals(" java.util.Map<String,String> arg0_priMap = null;", javaCodes.get(16));
        assertEquals(" arg0.setPriMap(arg0_priMap);", javaCodes.get(17));
        assertEquals("}", javaCodes.get(18));


        List<String> assertionJavaCode = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);

        assertEquals(13, assertionJavaCode.size());
        assertEquals("assertNull(res1.pubField);", assertionJavaCode.get(0));
        assertEquals("assertNull(res1.getPriField());", assertionJavaCode.get(1));
        assertEquals("assertNull(res1.getStringList());", assertionJavaCode.get(2));
        assertEquals("assertNull(res1.getPriEnum());", assertionJavaCode.get(3));
        assertEquals("assertNull(res1.getPriBoolean());", assertionJavaCode.get(4));
        assertEquals("assertEquals(false, res1.isPribool());", assertionJavaCode.get(5));
        assertEquals("assertNull(res1.getPriBByte());", assertionJavaCode.get(6));
        assertEquals("assertEquals(0, res1.getPribyte());", assertionJavaCode.get(7));
        assertEquals("assertNull(res1.getPriCharacter());", assertionJavaCode.get(8));
        assertEquals("assertEquals('\\u0000', res1.getPriChar());", assertionJavaCode.get(9));
        assertEquals("assertEquals(0, res1.getPriShort().shortValue());", assertionJavaCode.get(10));
        assertEquals("assertNull(res1.getPriSShort());", assertionJavaCode.get(11));
        assertEquals("assertNull(res1.getPriMap());", assertionJavaCode.get(12));

        NamedTypedValue res = endpoint.getResponse();
        PrivateFieldInResponseDto resInstance = new PrivateFieldInResponseDto(){{
            pubField = 42;
        }};

        res.setValueBasedOnInstance(resInstance);
        List<String> javaCodesForResponse = res.newInstanceWithJavaOrKotlin(true, true, "tmp", 0, true, true);
        assertEquals(7, javaCodesForResponse.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInResponseDto tmp = null;", javaCodesForResponse.get(0));
        assertEquals("{", javaCodesForResponse.get(1));
        assertEquals(" tmp = new com.thrift.example.artificial.PrivateFieldInResponseDto();", javaCodesForResponse.get(2));
        assertEquals(" tmp.pubField = 42;", javaCodesForResponse.get(3));
        assertEquals(" com.thrift.example.artificial.PrivateFieldInRequestDto tmp_priRequest = null;", javaCodesForResponse.get(4));
        assertEquals(" tmp.setPriRequest(tmp_priRequest);", javaCodesForResponse.get(5));
        assertEquals("}", javaCodesForResponse.get(6));

        List<String> assertionJavaCodeForResponse = res.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(2, assertionJavaCodeForResponse.size());
        assertEquals("assertEquals(42, res1.pubField);", assertionJavaCodeForResponse.get(0));
        assertEquals("assertNull(res1.getPriRequest());", assertionJavaCodeForResponse.get(1));

        PrivateFieldInResponseDto resInstance2 = new PrivateFieldInResponseDto(){{
            pubField = 42;
            setPriRequest(new PrivateFieldInRequestDto());
        }};

        res.setValueBasedOnInstance(resInstance2);

        javaCodesForResponse = res.newInstanceWithJavaOrKotlin(true, true, "tmp", 0, true, true);
        assertEquals(25, javaCodesForResponse.size());
        assertEquals("com.thrift.example.artificial.PrivateFieldInResponseDto tmp = null;", javaCodesForResponse.get(0));
        assertEquals("{", javaCodesForResponse.get(1));
        assertEquals(" tmp = new com.thrift.example.artificial.PrivateFieldInResponseDto();", javaCodesForResponse.get(2));
        assertEquals(" tmp.pubField = 42;", javaCodesForResponse.get(3));
        assertEquals(" com.thrift.example.artificial.PrivateFieldInRequestDto tmp_priRequest = null;", javaCodesForResponse.get(4));
        assertEquals(" {", javaCodesForResponse.get(5));
        assertEquals("  tmp_priRequest = new com.thrift.example.artificial.PrivateFieldInRequestDto();", javaCodesForResponse.get(6));
        assertEquals("  tmp_priRequest.setPubField(null);", javaCodesForResponse.get(7));
        assertEquals("  tmp_priRequest.setPriField(null);", javaCodesForResponse.get(8));
        assertEquals("  java.util.List<String> tmp_priRequest_stringList = null;", javaCodesForResponse.get(9));
        assertEquals("  tmp_priRequest.setStringList(tmp_priRequest_stringList);", javaCodesForResponse.get(10));
        assertEquals("  tmp_priRequest.setPriEnum(null);", javaCodesForResponse.get(11));
        assertEquals("  tmp_priRequest.setPriBoolean(null);", javaCodesForResponse.get(12));
        assertEquals("  tmp_priRequest.setPribool(false);", javaCodesForResponse.get(13));
        assertEquals("  tmp_priRequest.setPriBByte(null);", javaCodesForResponse.get(14));
        assertEquals("  tmp_priRequest.setPribyte(((byte)(0)));", javaCodesForResponse.get(15));
        assertEquals("  tmp_priRequest.setPriCharacter(null);", javaCodesForResponse.get(16));
        assertEquals("  tmp_priRequest.setPriChar('\\u0000');", javaCodesForResponse.get(17));
        assertEquals("  tmp_priRequest.setPriShort(((short)(0)));", javaCodesForResponse.get(18));
        assertEquals("  tmp_priRequest.setPriSShort(null);", javaCodesForResponse.get(19));
        assertEquals("  java.util.Map<String,String> tmp_priRequest_priMap = null;", javaCodesForResponse.get(20));
        assertEquals("  tmp_priRequest.setPriMap(tmp_priRequest_priMap);", javaCodesForResponse.get(21));
        assertEquals(" }", javaCodesForResponse.get(22));
        assertEquals(" tmp.setPriRequest(tmp_priRequest);", javaCodesForResponse.get(23));
        assertEquals("}", javaCodesForResponse.get(24));

        assertionJavaCodeForResponse = res.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(14, assertionJavaCodeForResponse.size());
        assertEquals("assertEquals(42, res1.pubField);", assertionJavaCodeForResponse.get(0));
        assertEquals("assertNull(res1.getPriRequest().pubField);", assertionJavaCodeForResponse.get(1));
        assertEquals("assertNull(res1.getPriRequest().getPriField());", assertionJavaCodeForResponse.get(2));
        assertEquals("assertNull(res1.getPriRequest().getStringList());", assertionJavaCodeForResponse.get(3));
        assertEquals("assertNull(res1.getPriRequest().getPriEnum());", assertionJavaCodeForResponse.get(4));
        assertEquals("assertNull(res1.getPriRequest().getPriBoolean());", assertionJavaCodeForResponse.get(5));
        assertEquals("assertEquals(false, res1.getPriRequest().isPribool());", assertionJavaCodeForResponse.get(6));
        assertEquals("assertNull(res1.getPriRequest().getPriBByte());", assertionJavaCodeForResponse.get(7));
        assertEquals("assertEquals(0, res1.getPriRequest().getPribyte());", assertionJavaCodeForResponse.get(8));
        assertEquals("assertNull(res1.getPriRequest().getPriCharacter());", assertionJavaCodeForResponse.get(9));
        assertEquals("assertEquals('\\u0000', res1.getPriRequest().getPriChar());", assertionJavaCodeForResponse.get(10));
        assertEquals("assertEquals(0, res1.getPriRequest().getPriShort().shortValue());", assertionJavaCodeForResponse.get(11));
        assertEquals("assertNull(res1.getPriRequest().getPriSShort());", assertionJavaCodeForResponse.get(12));
        assertEquals("assertNull(res1.getPriRequest().getPriMap());", assertionJavaCodeForResponse.get(13));
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

        List<String> javacode = p1.newInstanceWithJavaOrKotlin(0, true, true);
        assertEquals(5, javacode.size());
        assertEquals("java.util.Date arg0 = null;", javacode.get(0));
        assertEquals("{", javacode.get(1));
        assertEquals(" // Date is " + stringDate, javacode.get(2));
        assertEquals(" arg0 = new java.util.Date(" + time + "L);", javacode.get(3));
        assertEquals("}", javacode.get(4));

        List<String> assertions = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);
        assertEquals(1, assertions.size());
        assertTrue(assertions.get(0).contains("// runtime value is "));

    }


    @Test
    public void testLocalDateToString() throws ClassNotFoundException, ParseException {
        EndpointSchema endpoint = getOneEndpoint("localDateToString");
        assertNotNull(endpoint.getResponse());
        assertEquals(1, endpoint.getRequestParams().size());

        NamedTypedValue p1 = endpoint.getRequestParams().get(0);
        assertTrue(p1 instanceof DateParam);

        LocalDate date = LocalDate.of(2023, 8, 28);

        p1.setValueBasedOnInstance(date);
        Object instance = p1.newInstance();
        assertTrue(instance instanceof LocalDate);
        assertEquals(date.toString(), ((LocalDate) instance).toString());
        assertEquals(date.toEpochDay(), ((LocalDate) instance).toEpochDay());

        ParamDto dto = p1.getDto();
        assertEquals(3, dto.innerContent.size());
        assertEquals("2023", dto.innerContent.get(0).stringValue);
        assertEquals("8", dto.innerContent.get(1).stringValue);
        assertEquals("28", dto.innerContent.get(2).stringValue);


        List<String> javacode = p1.newInstanceWithJavaOrKotlin(0, true, true);

        String[] expectedContents = ("java.time.LocalDate arg0 = null;\n" +
            "{\n" +
            " // Date is 2023-08-28\n" +
            " arg0 = java.time.LocalDate.ofEpochDay(19597L);\n" +
            "}"
        ).split("\n");
        assertEquals(expectedContents.length, javacode.size());


        for (int i = 0; i < javacode.size(); i++)
            assertEquals(expectedContents[i], javacode.get(i));

        List<String> assertions = p1.newAssertionWithJavaOrKotlin(0, "res1", -1, true);

        List<String> expectedAssertions = Arrays.asList("// runtime value is 2023-08-28");
        assertEquals(expectedAssertions.size(), assertions.size());


        for (int i = 0; i < assertions.size(); i++)
            assertEquals(expectedAssertions.get(i), assertions.get(i));
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
