package org.evomaster.e2etests.spring.examples.impactXYZ;

import com.foo.rest.examples.spring.impactXYZ.ImpactXYZRestController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * created by manzh on 2020-06-16
 */
public class ImpactXYZManualTest extends SpringTestBase {

    @Test
    public void testSwaggerJSON() {

        given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/500?" +
                        "y=foo&z=bar")
                .then()
                .statusCode(500);


        String result  = given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/1500?" +
                        "y=bar&z=bar")
                .then()
                .statusCode(200)
                .extract().asString();
        assertEquals(result, "NOT_MATCHED");


        result = given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/1500?" +
                        "y=foo&z=bar")
                .then()
                .statusCode(200)
                .extract().asString();
        assertEquals(result, "CREATED_1");


        result = given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/15000?" +
                        "y=foo&z=bar")
                .then()
                .statusCode(200)
                .extract().asString();
        assertEquals(result, "CREATED_2");


        result = given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/25000?" +
                        "y=foo&z=bar")
                .then()
                .statusCode(200)
                .extract().asString();
        assertEquals(result, "CREATED_3");


        result = given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/35000?" +
                        "y=foo&z=bar")
                .then()
                .statusCode(200)
                .extract().asString();
        assertEquals(result, "CREATED_4");


        result = given().accept("*/*")
                .post(baseUrlOfSut+"/api/impactxyz/35000?" +
                        "y=foo&z=bar")
                .then()
                .statusCode(200)
                .extract().asString();
        assertEquals(result, "EXCEED");
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactXYZRestController(Arrays.asList("/api/impactdto/{x}")));
    }
}
