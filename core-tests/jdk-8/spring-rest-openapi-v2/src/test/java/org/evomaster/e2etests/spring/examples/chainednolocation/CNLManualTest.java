package org.evomaster.e2etests.spring.examples.chainednolocation;

import com.foo.rest.examples.spring.chainednolocation.CNL_X;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class CNLManualTest extends CNLTestBase {


    @Test
    public void testPostGet() {

        int value = 42;

        CNL_X x = new CNL_X();
        x.value = value;

        String id = given().contentType(ContentType.JSON)
                .body(x)
                .post(baseUrlOfSut + "/api/x")
                .then()
                .statusCode(201)
                .extract().body().path("id").toString();

        String location = baseUrlOfSut + "/api/x/"+  id;

        given().accept(ContentType.JSON)
                .get(location)
                .then()
                .statusCode(200)
                .body("value", CoreMatchers.equalTo(value));
    }
}
