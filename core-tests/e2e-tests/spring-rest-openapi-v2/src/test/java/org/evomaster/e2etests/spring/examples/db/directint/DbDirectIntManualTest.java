package org.evomaster.e2etests.spring.examples.db.directint;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class DbDirectIntManualTest extends DbDirectIntTestBase {


    @Test
    public void testEmpty(){

        given().accept(ContentType.ANY)
                .get(baseUrlOfSut + "/api/db/directint/42/77")
                .then()
                .statusCode(400);
    }

    @Test
    public void testNonEmpty(){

        given().accept(ContentType.ANY)
                .post(baseUrlOfSut + "/api/db/directint")
                .then()
                .statusCode(200);


        given().accept(ContentType.ANY)
                .get(baseUrlOfSut + "/api/db/directint/42/77")
                .then()
                .statusCode(200);
    }

}
