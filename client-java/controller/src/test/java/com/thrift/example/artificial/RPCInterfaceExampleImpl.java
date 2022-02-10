package com.thrift.example.artificial;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
}
