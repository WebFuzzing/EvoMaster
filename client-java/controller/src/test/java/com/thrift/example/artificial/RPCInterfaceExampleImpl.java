package com.thrift.example.artificial;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.transport.TTransportException;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/27
 */
public class RPCInterfaceExampleImpl implements RPCInterfaceExample{
    private boolean authorized = false;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    @Override
    public String simplePrimitive(int argInt, float argfloat, long arglong, double argdouble, char argchar, byte argbyte, boolean argboolean, short argshort) {
        return "int:"+argInt+",float:"+argfloat+",long:"+arglong+",double:"+argdouble+",char:"+argchar+",byte:"+argbyte+",boolean:"+argboolean+",short:"+argshort;
    }

    @Override
    public String simpleWrapPrimitive(Integer argInt, Float argfloat, Long arglong, Double argdouble, Character argchar, Byte argbyte, Boolean argboolean, Short argshort) {
        return "int:"+argInt+",float:"+argfloat+",long:"+arglong+",double:"+argdouble+",char:"+argchar+",byte:"+argbyte+",boolean:"+argboolean+",short:"+argshort;
    }

    @Override
    public GenericResponse array(List<String>[] args0) {
        GenericResponse response = new GenericResponse();
        response.info = Arrays.stream(args0).map(s-> String.join(",", s)).collect(Collectors.joining(";"));
        return response;
    }

    @Override
    public GenericResponse arrayboolean(boolean[] args0) {
        GenericResponse response = new GenericResponse();
        StringBuffer sb = new StringBuffer();
        for (boolean b : args0){
            sb.append(b+",");
        }
        sb.append("ARRAY_END");
        response.info = sb.toString();
        return response;
    }

    @Override
    public GenericResponse list(List<String> args0) {
        GenericResponse response = new GenericResponse();
        response.info = String.join(",", args0);
        return response;
    }

    @Override
    public GenericResponse map(Map<String, String> args0) {
        GenericResponse response = new GenericResponse();
        response.info = args0.entrySet().stream().map(s-> s.getKey()+":"+s.getValue()).collect(Collectors.joining(","));
        return response;
    }

    @Override
    public GenericResponse listAndMap(List<Map<String, String>> args0) {
        GenericResponse response = new GenericResponse();
        response.info = args0.stream()
                .map(l-> l.entrySet().stream().map(s-> s.getKey()+":"+s.getValue()).collect(Collectors.joining(",")))
                .collect(Collectors.joining(";"));
        return response;
    }

    @Override
    public ObjectResponse objResponse() {
        ObjectResponse response = new ObjectResponse();
        response.f1 = "foo";
        response.f2 = 42;
        response.f3 = 0.42;
        response.f4 = new double[]{0.0, 0.5, 1.0};
        response.systemTime = System.nanoTime();
        return response;
    }

    @Override
    public CycleAObj objCycleA() {
        return null;
    }

    @Override
    public CycleBObj objCycleB() {
        return null;
    }

    @Override
    public String dateToString(Date date) {
        return date.toString();
    }

    @Override
    public String localDateToString(LocalDate date) {
        return date.toString();
    }

    @Override
    public String constraintInputs(ConstrainedRequest arg0, String arg1) {
        return null;
    }

    @Override
    public String handleCustomizedRequestA(CustomizedRequestA request) {
        return null;
    }

    @Override
    public String handleCustomizedRequestB(CustomizedRequestB request) {
        return null;
    }

    @Override
    public void login(AuthLoginDto dto) {

    }

    @Override
    public PrivateFieldInResponseDto accessFieldDtoCheck(PrivateFieldInRequestDto dto) {
        return null;
    }

    @Override
    public ByteResponse byteResponse(byte arg1, Byte arg2) {
        ByteResponse res = new ByteResponse();
        res.byteValue = arg2;
        res.pbyteValue = arg1;
        return res;
    }

    @Override
    public String authorizedEndpoint() {
        if (authorized)
            return "local";
        return null;
    }

    @Override
    public void throwRuntimeException() {
        throw new RuntimeException("runtime exception");
    }

    @Override
    public void throwUndeclaredThrowableException() {
        throw new UndeclaredThrowableException(new IllegalStateException("undeclared"));
    }

    private final String child_mark = "child";

    @Override
    public StringChildDto handledInheritedGenericStringDto(StringChildDto dto) {
        if (dto == null) return null;
        dto.setCode(dto.getCode()!= null? child_mark+dto.getCode(): child_mark);
        dto.setMessage(dto.getMessage()!=null? child_mark+ dto.getMessage(): child_mark);
        return dto;
    }

    @Override
    public IntChildDto handledInheritedGenericIntDto(IntChildDto dto) {
        if (dto == null) return null;

        dto.setCode(dto.getCode()!= null? 1+dto.getCode(): 0);
        dto.setMessage(dto.getMessage()!=null? 1+ dto.getMessage(): 0);
        return dto;
    }

    @Override
    public ListChildDto handledInheritedGenericListDto(ListChildDto dto) {
        if (dto == null) return null;
        dto.setCode(dto.getCode()!= null? dto.getCode().stream().map(x-> x+1).collect(Collectors.toList()): Arrays.asList(0));
        dto.setMessage(dto.getMessage()!=null? dto.getCode().stream().map(x-> x+1).collect(Collectors.toList()): Arrays.asList(0));
        return dto;
    }

    @Override
    public GenericDto<Integer, String> handleGenericIntString(GenericDto<Integer, String> dto) {
        if (dto == null) return null;
        dto.data1 = dto.data1 == null? 0 : dto.data1+1;
        dto.data2 = dto.data2 == null? "generic" : "generic"+dto.data2;
        return dto;
    }

    @Override
    public GenericDto<StringChildDto, String> handleGenericObjectString(GenericDto<StringChildDto, String> dto) {
        if (dto == null) return null;
        if (dto.data1 == null)
            dto.data1 = new StringChildDto(){{
                setMessage(child_mark);
                setCode(child_mark);
            }};
        else{
         dto.data1 = handledInheritedGenericStringDto(dto.data1);
        }
        dto.data2 =  dto.data2 == null? "generic" : "generic"+dto.data2;
        return dto;
    }

    @Override
    public NestedGenericDto<String> handleNestedGenericString(NestedGenericDto<String> dto) {
        if (dto.intData == null){
            dto.intData = new GenericDto<String, Integer>(){{
                data1 = child_mark;
                data2 = 0;
            }};
        }
        if (dto.stringData == null){
            dto.stringData = new GenericDto<String, String>(){{
                data1 = child_mark;
                data2 = child_mark;
            }};
        }
        if (dto.list == null){
            dto.list = Arrays.asList(child_mark, child_mark);
        }

        if (dto.set == null){
            dto.set = new HashSet<String>(){{
                add(child_mark);
            }};
        }

        return dto;
    }

    @Override
    public void handleException(String type) throws Exception {
        if (type == null)
            throw new NullPointerException("null");
        if (type.equals("state"))
            throw new IllegalStateException(type);
        if (type.equals("argument"))
            throw new IllegalArgumentException(type);
        throw new RuntimeException(type);
    }

    @Override
    public String handleEnumWithConstructor(ObjectEnum arg1) {
        if (arg1 == null || arg1.enumWithConstructor == null) return null;
        return arg1.enumWithConstructor.getDesc();
    }

    @Override
    public String bigNumber(BigNumberObj arg1) {
        if (arg1 == null) return null;
        return arg1.toString();
    }

    @Override
    public String immutableObj(ImmutableObj arg1) {
        if (arg1 == null) return null;
        return arg1.toString();
    }

    @Override
    public String numericString(NumericStringObj arg1) {
        if (arg1 == null) return null;
        return arg1.toString();
    }

    @Override
    public Map<String, NumericStringObj> mapResponse() {
        return new HashMap<String, NumericStringObj>(){{
            put("foo", new NumericStringObj(){{
                setIntValue("42");
                setLongValue("42L");
                setBigDecimalValue("42.42");
                setBigIntegerValue("4242");
            }});

            put("bar", new NumericStringObj(){{
                setIntValue("2");
                setLongValue("2L");
                setBigDecimalValue("2.42");
                setBigIntegerValue("242");
            }});
        }};
    }

    @Override
    public List<BigNumberObj> listResponse() {
        return Arrays.asList(
                new BigNumberObj(){{
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
                }}
        );
    }

    @Override
    public boolean pBoolResponse() {
        return false;
    }

    @Override
    public byte pByteResponse() {
        return 0;
    }

    @Override
    public char pCharResponse() {
        return 0;
    }

    @Override
    public short pShortResponse() {
        return 0;
    }

    @Override
    public int pIntResponse() {
        return 0;
    }

    @Override
    public long pLongResponse() {
        return 0;
    }

    @Override
    public float pFloatResponse() {
        return 0;
    }

    @Override
    public double pDoubleResponse() {
        return 0;
    }

    @Override
    public String seedcheck(List<Long> longList, List<Integer> integerList, List<BigNumberObj> objList, Map<Integer, String> integerStringMap, BigNumberObj obj) {
        StringBuilder sb = new StringBuilder();
        if (longList != null){
            longList.forEach(l-> sb.append(l).append(";"));
            sb.append(System.lineSeparator());
        }

        if (integerList != null){
            integerList.forEach(l-> sb.append(l).append(";"));
            sb.append(System.lineSeparator());
        }

        if (objList != null){
            objList.forEach(l-> sb.append(l.toString()).append(";"));
            sb.append(System.lineSeparator());
        }

        if (integerStringMap != null){
            integerStringMap.forEach((key, value) -> sb.append(key).append(":").append(value).append(";"));
            sb.append(System.lineSeparator());
        }

        if (obj != null)
            sb.append(obj).append(";");

        return sb.toString();
    }

    @Override
    public boolean throwTException(int type) throws Exception {
        if (type == 0)
            throw new TException("Base-TException");
        if (type == 1)
            throw new TApplicationException(TApplicationException.INTERNAL_ERROR, "TAPP-internal");
        if (type == 2)
            throw new TApplicationException(TApplicationException.PROTOCOL_ERROR, "TAPP-protocol");
        if (type == 3)
            throw new TProtocolException("TProtocol");
        if (type == 4)
            throw new TTransportException("TTransport");
        throw new Exception("general");
    }
}
