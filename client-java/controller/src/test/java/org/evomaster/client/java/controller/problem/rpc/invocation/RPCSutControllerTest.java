package org.evomaster.client.java.controller.problem.rpc.invocation;

import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.ActionResponseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCInterfaceSchemaDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

/**
 * created by manzhang on 2021/11/27
 */
public class RPCSutControllerTest {

    public final static FakeSutController rpcController = new FakeSutController();

    private static List<RPCInterfaceSchemaDto> interfaceSchemas;

    @BeforeAll
    public static void initClass() {
        rpcController.setControllerPort(0);
        rpcController.startTheControllerServer();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = rpcController.getControllerServerPort();
        RestAssured.basePath = "/controller/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();


        interfaceSchemas = given()
                .accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("data.isSutRunning", is(false))
                .extract().body().jsonPath().getList("data.rpcProblem.schemas.", RPCInterfaceSchemaDto.class);
    }

    @AfterAll
    public static void tearDown() {
        rpcController.stopSut();
    }


    @Test
    public void testHandledInheritedGenericStringDto(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("handledInheritedGenericStringDto")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(1, dto.requestParams.size());
        dto.doGenerateAssertions = true;
        dto.doGenerateTestScript = true;
        dto.controllerVariable = "controller";
        dto.responseVariable = "res1";

        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);

        assertEquals(10, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.StringChildDto res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" com.thrift.example.artificial.StringChildDto arg0 = null;", responseDto.testScript.get(2));
        assertEquals(" {", responseDto.testScript.get(3));
        assertEquals("  arg0 = new com.thrift.example.artificial.StringChildDto();", responseDto.testScript.get(4));
        assertEquals("  arg0.setCode(null);", responseDto.testScript.get(5));
        assertEquals("  arg0.setMessage(null);", responseDto.testScript.get(6));
        assertEquals(" }", responseDto.testScript.get(7));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).handledInheritedGenericStringDto(arg0);", responseDto.testScript.get(8));
        assertEquals("}", responseDto.testScript.get(9));

        assertEquals(2, responseDto.assertionScript.size());
        assertEquals("assertEquals(\"child\", res1.getCode());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(\"child\", res1.getMessage());", responseDto.assertionScript.get(1));

        ParamDto request = dto.requestParams.get(0);
        assertEquals(2, request.innerContent.size());
        request.innerContent.get(0).stringValue = "ppcode";
        request.innerContent.get(1).stringValue = "pmsg";

        responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);

        assertEquals(10, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.StringChildDto res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" com.thrift.example.artificial.StringChildDto arg0 = null;", responseDto.testScript.get(2));
        assertEquals(" {", responseDto.testScript.get(3));
        assertEquals("  arg0 = new com.thrift.example.artificial.StringChildDto();", responseDto.testScript.get(4));
        assertEquals("  arg0.setCode(((java.lang.String)(\"ppcode\")));", responseDto.testScript.get(5));
        assertEquals("  arg0.setMessage(((java.lang.String)(\"pmsg\")));", responseDto.testScript.get(6));
        assertEquals(" }", responseDto.testScript.get(7));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).handledInheritedGenericStringDto(arg0);", responseDto.testScript.get(8));
        assertEquals("}", responseDto.testScript.get(9));

        assertEquals(2, responseDto.assertionScript.size());
        assertEquals("assertEquals(\"childppcode\", res1.getCode());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(\"childpmsg\", res1.getMessage());", responseDto.assertionScript.get(1));
    }


    @Test
    public void testHandledInheritedGenericIntDto(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("handledInheritedGenericIntDto")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(1, dto.requestParams.size());
        dto.doGenerateAssertions = true;
        dto.doGenerateTestScript = true;
        dto.controllerVariable = "controller";
        dto.responseVariable = "res1";

        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);

        assertEquals(10, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.IntChildDto res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" com.thrift.example.artificial.IntChildDto arg0 = null;", responseDto.testScript.get(2));
        assertEquals(" {", responseDto.testScript.get(3));
        assertEquals("  arg0 = new com.thrift.example.artificial.IntChildDto();", responseDto.testScript.get(4));
        assertEquals("  arg0.setCode(null);", responseDto.testScript.get(5));
        assertEquals("  arg0.setMessage(null);", responseDto.testScript.get(6));
        assertEquals(" }", responseDto.testScript.get(7));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).handledInheritedGenericIntDto(arg0);", responseDto.testScript.get(8));
        assertEquals("}", responseDto.testScript.get(9));

        assertEquals(2, responseDto.assertionScript.size());
        assertEquals("assertEquals(0, res1.getCode().intValue());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(0, res1.getMessage().intValue());", responseDto.assertionScript.get(1));

        ParamDto request = dto.requestParams.get(0);
        assertEquals(2, request.innerContent.size());
        request.innerContent.get(0).stringValue = "1";
        request.innerContent.get(1).stringValue = "2";

        responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);

        assertEquals(10, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.IntChildDto res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" com.thrift.example.artificial.IntChildDto arg0 = null;", responseDto.testScript.get(2));
        assertEquals(" {", responseDto.testScript.get(3));
        assertEquals("  arg0 = new com.thrift.example.artificial.IntChildDto();", responseDto.testScript.get(4));
        assertEquals("  arg0.setCode(((java.lang.Integer)(1)));", responseDto.testScript.get(5));
        assertEquals("  arg0.setMessage(((java.lang.Integer)(2)));", responseDto.testScript.get(6));
        assertEquals(" }", responseDto.testScript.get(7));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).handledInheritedGenericIntDto(arg0);", responseDto.testScript.get(8));
        assertEquals("}", responseDto.testScript.get(9));

        assertEquals(2, responseDto.assertionScript.size());
        assertEquals("assertEquals(2, res1.getCode().intValue());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(3, res1.getMessage().intValue());", responseDto.assertionScript.get(1));
    }


    @Test
    public void testHandledInheritedGenericListDto(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("handledInheritedGenericListDto")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(1, dto.requestParams.size());
        dto.doGenerateAssertions = true;
        dto.doGenerateTestScript = true;
        dto.controllerVariable = "controller";
        dto.responseVariable = "res1";
        dto.maxAssertionForDataInCollection = 10;

        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);

        assertEquals(12, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.ListChildDto res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" com.thrift.example.artificial.ListChildDto arg0 = null;", responseDto.testScript.get(2));
        assertEquals(" {", responseDto.testScript.get(3));
        assertEquals("  arg0 = new com.thrift.example.artificial.ListChildDto();", responseDto.testScript.get(4));
        assertEquals("  java.util.List<java.lang.Integer> arg0_code = null;", responseDto.testScript.get(5));
        assertEquals("  arg0.setCode(arg0_code);", responseDto.testScript.get(6));
        assertEquals("  java.util.List<java.lang.Integer> arg0_message = null;", responseDto.testScript.get(7));
        assertEquals("  arg0.setMessage(arg0_message);", responseDto.testScript.get(8));
        assertEquals(" }", responseDto.testScript.get(9));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).handledInheritedGenericListDto(arg0);", responseDto.testScript.get(10));
        assertEquals("}", responseDto.testScript.get(11));

        assertEquals(4, responseDto.assertionScript.size());
        assertEquals("assertEquals(1, res1.getCode().size());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(0, res1.getCode().get(0).intValue());", responseDto.assertionScript.get(1));
        assertEquals("assertEquals(1, res1.getMessage().size());", responseDto.assertionScript.get(2));
        assertEquals("assertEquals(0, res1.getMessage().get(0).intValue());", responseDto.assertionScript.get(3));

        ParamDto request = dto.requestParams.get(0);
        assertEquals(2, request.innerContent.size());
        ParamDto innerCode = request.innerContent.get(0).type.example.copy();
        innerCode.stringValue = "1";
        request.innerContent.get(0).innerContent = Arrays.asList(innerCode);
        ParamDto innerMsg = request.innerContent.get(1).type.example.copy();
        innerMsg.stringValue = "2";
        request.innerContent.get(1).innerContent = Arrays.asList(innerMsg);

        responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);

        assertEquals(22, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.ListChildDto res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" com.thrift.example.artificial.ListChildDto arg0 = null;", responseDto.testScript.get(2));
        assertEquals(" {", responseDto.testScript.get(3));
        assertEquals("  arg0 = new com.thrift.example.artificial.ListChildDto();", responseDto.testScript.get(4));
        assertEquals("  java.util.List<java.lang.Integer> arg0_code = null;", responseDto.testScript.get(5));
        assertEquals("  {", responseDto.testScript.get(6));
        assertEquals("   arg0_code = new java.util.ArrayList<>();", responseDto.testScript.get(7));
        assertEquals("   java.lang.Integer arg0_code_e_0 = 1;", responseDto.testScript.get(8));
        assertEquals("   arg0_code.add(arg0_code_e_0);", responseDto.testScript.get(9));
        assertEquals("  }", responseDto.testScript.get(10));
        assertEquals("  arg0.setCode(arg0_code);", responseDto.testScript.get(11));
        assertEquals("  java.util.List<java.lang.Integer> arg0_message = null;", responseDto.testScript.get(12));
        assertEquals("  {", responseDto.testScript.get(13));
        assertEquals("   arg0_message = new java.util.ArrayList<>();", responseDto.testScript.get(14));
        assertEquals("   java.lang.Integer arg0_message_e_0 = 2;", responseDto.testScript.get(15));
        assertEquals("   arg0_message.add(arg0_message_e_0);", responseDto.testScript.get(16));
        assertEquals("  }", responseDto.testScript.get(17));
        assertEquals("  arg0.setMessage(arg0_message);", responseDto.testScript.get(18));
        assertEquals(" }", responseDto.testScript.get(19));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).handledInheritedGenericListDto(arg0);", responseDto.testScript.get(20));
        assertEquals("}", responseDto.testScript.get(21));

        assertEquals(4, responseDto.assertionScript.size());
        assertEquals("assertEquals(1, res1.getCode().size());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(2, res1.getCode().get(0).intValue());", responseDto.assertionScript.get(1));
        assertEquals("assertEquals(1, res1.getMessage().size());", responseDto.assertionScript.get(2));
        assertEquals("assertEquals(3, res1.getMessage().get(0).intValue());", responseDto.assertionScript.get(3));
    }


    @Test
    public void testRuntimeException(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("throwRuntimeException")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);
        assertNotNull(responseDto.exceptionInfoDto);
        assertEquals("java.lang.RuntimeException", responseDto.exceptionInfoDto.exceptionName);
        assertEquals("runtime exception", responseDto.exceptionInfoDto.exceptionMessage);
        assertFalse(responseDto.exceptionInfoDto.isCauseOfUndeclaredThrowable);
    }

    @Test
    public void testUndeclaredThrowableException(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("throwUndeclaredThrowableException")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);
        assertNotNull(responseDto.exceptionInfoDto);
        assertEquals("java.lang.IllegalStateException", responseDto.exceptionInfoDto.exceptionName);
        assertEquals("undeclared", responseDto.exceptionInfoDto.exceptionMessage);
        assertTrue(responseDto.exceptionInfoDto.isCauseOfUndeclaredThrowable);
    }

    @Test
    public void testLocalAuth(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("authorizedEndpoint")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        RPCActionDto localDto = rpcController.getLocalAuthSetupSchemaMap().get(0).getDto();

        localDto.responseVariable = "res1_auth";
        localDto.doGenerateTestScript = true;
        localDto.controllerVariable = "controller";
        ActionResponseDto authResponseDto = new ActionResponseDto();
        rpcController.executeHandleLocalAuthenticationSetup(localDto, authResponseDto);
        assertNotNull(authResponseDto.testScript);
        assertEquals(4, authResponseDto.testScript.size());
        assertEquals("{", authResponseDto.testScript.get(0));
        assertEquals(" java.lang.String arg0 = \"local_foo\";", authResponseDto.testScript.get(1));
        assertEquals(" controller.handleLocalAuthenticationSetup(arg0);", authResponseDto.testScript.get(2));
        assertEquals("}", authResponseDto.testScript.get(3));


        dto.doGenerateAssertions = true;
        dto.doGenerateTestScript = true;
        dto.controllerVariable = "controller";
        dto.responseVariable = "res1";
        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);
        assertNotNull(responseDto.assertionScript);
        assertEquals("assertEquals(\"local\", res1);", responseDto.assertionScript.get(0));

    }

    @Test
    public void testSutInfoAndSchema(){
        assertEquals(1, interfaceSchemas.size());
    }

    @Test
    public void testSimpleWrapPrimitiveEndpoint(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("simpleWrapPrimitive")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(8, dto.requestParams.size());
        dto.requestParams.get(0).stringValue = ""+42;
        dto.requestParams.get(1).stringValue = ""+4.2f;
        dto.requestParams.get(2).stringValue = ""+42L;
        dto.requestParams.get(3).stringValue = ""+4.2;
        dto.requestParams.get(4).stringValue = ""+'x';
        dto.requestParams.get(5).stringValue = ""+ Byte.parseByte("42");
        dto.requestParams.get(6).stringValue = ""+ false;
        dto.requestParams.get(7).stringValue = ""+ Short.parseShort("42");
        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);
        assertNotNull(responseDto.rpcResponse);
        assertEquals(RPCSupportedDataType.STRING, responseDto.rpcResponse.type.type);
        assertEquals("int:42,float:4.2,long:42,double:4.2,char:x,byte:42,boolean:false,short:42", responseDto.rpcResponse.stringValue);
    }

    @Test
    public void testSimplePrimitiveEndpoint(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("simplePrimitive")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(8, dto.requestParams.size());
        dto.requestParams.get(0).stringValue = ""+42;
        dto.requestParams.get(1).stringValue = ""+4.2f;
        dto.requestParams.get(2).stringValue = ""+42L;
        dto.requestParams.get(3).stringValue = ""+4.2;
        dto.requestParams.get(4).stringValue = ""+'x';
        dto.requestParams.get(5).stringValue = ""+ Byte.parseByte("42");
        dto.requestParams.get(6).stringValue = ""+ false;
        dto.requestParams.get(7).stringValue = ""+ Short.parseShort("42");
        ActionResponseDto responseDto = new ActionResponseDto();
        rpcController.executeAction(dto, responseDto);
        assertNotNull(responseDto.rpcResponse);
        assertEquals(RPCSupportedDataType.STRING, responseDto.rpcResponse.type.type);
        assertEquals("int:42,float:4.2,long:42,double:4.2,char:x,byte:42,boolean:false,short:42", responseDto.rpcResponse.stringValue);
    }


    @Test
    public void testByteResponse(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionName.equals("byteResponse")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        dto.requestParams.get(0).stringValue = "" + Byte.parseByte("0");
        dto.requestParams.get(1).stringValue = "" + Byte.parseByte("42");
        assertEquals(2, dto.requestParams.size());
        ActionResponseDto responseDto = new ActionResponseDto();
        dto.doGenerateTestScript = true;
        dto.doGenerateAssertions = true;
        dto.controllerVariable = "controller";
        dto.responseVariable = "res1";
        rpcController.executeAction(dto, responseDto);
        assertNotNull(responseDto.rpcResponse);
        assertEquals(RPCSupportedDataType.CUSTOM_OBJECT, responseDto.rpcResponse.type.type);
        assertEquals(6, responseDto.testScript.size());
        assertEquals("com.thrift.example.artificial.ByteResponse res1 = null;", responseDto.testScript.get(0));
        assertEquals("{", responseDto.testScript.get(1));
        assertEquals(" byte arg0 = 0;", responseDto.testScript.get(2));
        assertEquals(" java.lang.Byte arg1 = 42;", responseDto.testScript.get(3));
        assertEquals(" res1 = ((com.thrift.example.artificial.RPCInterfaceExampleImpl)(controller.getRPCClient(\"com.thrift.example.artificial.RPCInterfaceExample\"))).byteResponse(arg0,arg1);", responseDto.testScript.get(4));
        assertEquals("}", responseDto.testScript.get(5));
        assertEquals(2, responseDto.assertionScript.size());
        assertEquals("assertEquals(42, res1.byteValue.byteValue());", responseDto.assertionScript.get(0));
        assertEquals("assertEquals(0, res1.pbyteValue);", responseDto.assertionScript.get(1));
        responseDto.testScript.forEach(System.out::println);
        responseDto.assertionScript.forEach(System.out::println);

    }
}
