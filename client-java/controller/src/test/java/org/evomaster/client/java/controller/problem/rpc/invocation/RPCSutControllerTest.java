package org.evomaster.client.java.controller.problem.rpc.invocation;

import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.ActionResponseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCInterfaceSchemaDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
