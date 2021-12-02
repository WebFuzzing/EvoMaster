package org.evomaster.client.java.controller.problem.rpc.invocation;

import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCInterfaceSchemaDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;


import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionId.equals("simpleWrapPrimitive")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(8, dto.requestParams.size());
        dto.requestParams.get(0).jsonValue = ""+42;
        dto.requestParams.get(1).jsonValue = ""+4.2f;
        dto.requestParams.get(2).jsonValue = ""+42L;
        dto.requestParams.get(3).jsonValue = ""+4.2;
        dto.requestParams.get(4).jsonValue = ""+'x';
        dto.requestParams.get(5).jsonValue = ""+ Byte.parseByte("42");
        dto.requestParams.get(6).jsonValue = ""+ false;
        dto.requestParams.get(7).jsonValue = ""+ Short.parseShort("42");
        String result = (String)rpcController.executeAction(dto);
        assertEquals("int:42,float:4.2,long:42,double:4.2,char:x,byte:42,boolean:false,short:42", result);
    }

    @Test
    public void testSimplePrimitiveEndpoint(){
        List<RPCActionDto> dtos = interfaceSchemas.get(0).endpoints.stream().filter(s-> s.actionId.equals("simplePrimitive")).collect(Collectors.toList());
        assertEquals(1, dtos.size());
        RPCActionDto dto = dtos.get(0).copy();
        assertEquals(8, dto.requestParams.size());
        dto.requestParams.get(0).jsonValue = ""+42;
        dto.requestParams.get(1).jsonValue = ""+4.2f;
        dto.requestParams.get(2).jsonValue = ""+42L;
        dto.requestParams.get(3).jsonValue = ""+4.2;
        dto.requestParams.get(4).jsonValue = ""+'x';
        dto.requestParams.get(5).jsonValue = ""+ Byte.parseByte("42");
        dto.requestParams.get(6).jsonValue = ""+ false;
        dto.requestParams.get(7).jsonValue = ""+ Short.parseShort("42");
        String result = (String)rpcController.executeAction(dto);
        assertEquals("int:42,float:4.2,long:42,double:4.2,char:x,byte:42,boolean:false,short:42", result);
    }


}
