package com.thrift.example.artificial;

import org.apache.thrift.TException;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * created by manzhang on 2021/11/15
 */
public interface RPCInterfaceExample {

    String simplePrimitive(int argInt, float argfloat, long arglong, double argdouble, char argchar, byte argbyte, boolean argboolean, short argshort);

    String simpleWrapPrimitive(Integer argInt, Float argfloat, Long arglong, Double argdouble, Character argchar, Byte argbyte, Boolean argboolean, Short argshort);

    GenericResponse array(List<String>[] args0);

    GenericResponse arrayboolean(boolean[] args0);

    GenericResponse list(List<String> args0);

    GenericResponse map(Map<String, String> args0);

    GenericResponse listAndMap(List<Map<String, String>> args0);

    ObjectResponse objResponse();

    CycleAObj objCycleA();

    CycleBObj objCycleB();

    String dateToString(Date date);

    String localDateToString(LocalDate date);

    String constraintInputs(ConstrainedRequest arg0, @NotNull String arg1);


    String handleCustomizedRequestA(CustomizedRequestA request);

    String handleCustomizedRequestB(CustomizedRequestB request);

    void login(AuthLoginDto dto);

    PrivateFieldInResponseDto accessFieldDtoCheck(PrivateFieldInRequestDto dto);

    ByteResponse byteResponse(byte arg1, Byte arg2);

    String authorizedEndpoint();

    void throwRuntimeException();

    void throwUndeclaredThrowableException();

    StringChildDto handledInheritedGenericStringDto(StringChildDto dto);

    IntChildDto handledInheritedGenericIntDto(IntChildDto dto);

    ListChildDto handledInheritedGenericListDto(ListChildDto dto);

    GenericDto<Integer, String> handleGenericIntString(GenericDto<Integer, String> dto);

    GenericDto<StringChildDto, String> handleGenericObjectString(GenericDto<StringChildDto, String> dto);

    NestedGenericDto<String> handleNestedGenericString(NestedGenericDto<String> dto);

    void handleException(String type) throws Exception;

    String handleEnumWithConstructor(ObjectEnum arg1);

    String bigNumber(BigNumberObj arg1);

    String immutableObj(ImmutableObj arg1);

    String numericString(NumericStringObj arg1);

    Map<String, NumericStringObj> mapResponse();

    List<BigNumberObj> listResponse();

    boolean pBoolResponse();

    byte pByteResponse();

    char pCharResponse();

    short pShortResponse();

    int pIntResponse();

    long pLongResponse();

    float pFloatResponse();

    double pDoubleResponse();

    String seedcheck(List<Long> longList, List<Integer> integerList, List<BigNumberObj> objList, Map<Integer, String> integerStringMap, BigNumberObj obj);

    boolean throwTException(int type) throws Exception;

}
