package org.evomaster.e2etests.spring.examples.chainedheaderlocation;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controllerApi.EMTestUtils.isValidURIorEmpty;
import static org.evomaster.clientJava.controllerApi.EMTestUtils.resolveLocation;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CHLManualTest extends CHLTestBase {


    @Test
    public void testPostGet() {


        String location_0_idx = "";
        String location_1_idy = "";
        String location_2_idz = "";

        location_0_idx = given()
                .post(baseUrlOfSut + "/api/chl/x")
                .then()
                .statusCode(201)
                .extract().header("location");

        assertTrue(isValidURIorEmpty(location_0_idx));


        location_1_idy = given()
                .post(resolveLocation(location_0_idx, baseUrlOfSut + "/api/chl/x/{idx}/y"))
                .then()
                .statusCode(201)
                .extract().header("location");

        assertTrue(isValidURIorEmpty(location_1_idy));


        location_2_idz = given()
                .post(resolveLocation(location_1_idy, baseUrlOfSut + "/api/chl/x/{idx}/y/{idy}/z"))
                .then()
                .statusCode(201)
                .extract().header("location");

        assertTrue(isValidURIorEmpty(location_2_idz));


        given().get(resolveLocation(location_2_idz, baseUrlOfSut + "/api/chl/x/{idx}/y/{idy}/z/{idz}/value"))
                .then()
                .statusCode(200);
    }
}