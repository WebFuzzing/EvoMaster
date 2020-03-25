package org.evomaster.e2etests.spring.examples.expectations;

import io.restassured.response.ValidatableResponse;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.expect.ExpectationHandler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.expect.ExpectationHandler.expectationHandler;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpectationsManualTest extends ExpectationsTestBase{

    @Test
    public void testRun(){

        ExpectationHandler expectationHandler = expectationHandler();

        ValidatableResponse call_0 = given().accept("*/*")
                .get(baseUrlOfSut + "/api/expectations/getExpectations/true")
                .then()
                .statusCode(200)
                .body(containsString("True"));
        expectationHandler.expect(true)
                .that(true, Arrays.asList(200, 401, 403, 404).contains(call_0.extract().statusCode()));

        ValidatableResponse call_1 = given().accept("*/*")
                .get(baseUrlOfSut + "/api/expectations/getExpectations/false")
                .then()
                .statusCode(500);

        // BMR: since I'm trying to make sure that that particular assertion fails (since a 500 is not among supported status codes, I've wrapped it thus. Will refactor as soon as I can think of a more elegant option.
        try {
            expectationHandler.expect(true)
                    .that(true, Arrays.asList(200, 401, 403, 404).contains(call_1.extract().statusCode()));
        }
        catch (AssertionError assertionError){
            assertTrue(assertionError.getMessage().equalsIgnoreCase("Failed Expectation Exception"));
        }

    }
}
