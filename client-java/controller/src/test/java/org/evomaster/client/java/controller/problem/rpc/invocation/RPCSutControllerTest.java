package org.evomaster.client.java.controller.problem.rpc.invocation;

import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCInterfaceSchemaDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;


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

//    @Test
//    public void testArrayEndpoint(){
//        EndpointSchema endpoint = interfaceSchemas.get(0).findEndpoints("array").get(0);
//        RPCActionDto rpcActionDto = new RPCActionDto();
//        rpcActionDto.rpcCall = endpoint;
//        rpcActionDto.interfaceId = "com.thrift.example.artificial.RPCInterfaceExample";

//        assertEquals(1, endpoint.requestParams.size());
//        NamedTypedValue param = endpoint.requestParams.get(0);
//        assertTrue(param instanceof ArrayParam);
//        assertTrue(param.getType() instanceof CollectionType);
//        NamedTypedValue template = ((CollectionType) param.getType()).getTemplate();
//        assertTrue(template instanceof ListParam);
//        assertTrue(template.getType() instanceof CollectionType);
//        NamedTypedValue element = ((CollectionType) template.getType()).getTemplate();
//        assertTrue(element instanceof StringParam);
//
//        StringParam str1 = ((StringParam) element).copyStructure();
//        str1.setValue("foo");
//        StringParam str2 = ((StringParam) element).copyStructure();
//        str1.setValue("bar");
//
//        ListParam list = ((ListParam) template).copyStructure();
//        list.setValue(Arrays.asList(str1, str2));
//
//        param.setValue(Arrays.asList(list));
//
//        Object res = rpcController.executeAction(rpcActionDto);
//        System.out.println(res);
//    }
}
