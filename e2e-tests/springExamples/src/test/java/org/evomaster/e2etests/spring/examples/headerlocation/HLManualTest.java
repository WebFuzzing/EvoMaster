package org.evomaster.e2etests.spring.examples.headerlocation;

import com.foo.rest.examples.spring.headerlocation.HeaderLocationDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controllerApi.EMTestUtils.isValidURIorEmpty;
import static org.evomaster.clientJava.controllerApi.EMTestUtils.resolveLocation;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HLManualTest extends HLTestBase {


    @Test
    public void testPostGet() {

        String name = "aName";
        String value = "foo";

        String location = "";

        location = given().contentType(ContentType.JSON)
                .body(new HeaderLocationDto(name, value))
                .post(baseUrlOfSut + "/api/hl")
                .then()
                .statusCode(201)
                .extract().header("location");

        assertTrue(isValidURIorEmpty(location));


        given().accept(ContentType.JSON)
                .get(resolveLocation(location, baseUrlOfSut + "/api/hl/{id}"))
                .then()
                .statusCode(200)
                .body("value", is(value));
    }
}
