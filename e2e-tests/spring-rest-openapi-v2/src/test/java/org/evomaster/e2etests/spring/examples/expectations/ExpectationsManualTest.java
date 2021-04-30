package org.evomaster.e2etests.spring.examples.expectations;

import com.foo.rest.examples.spring.expectations.ExpectationsController;
import io.restassured.response.ValidatableResponse;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.expect.ExpectationHandler;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.expect.ExpectationHandler.expectationHandler;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpectationsManualTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception{

        SpringTestBase.initClass(new ExpectationsController());
    }

    @Test
    public void testRun(){

        ExpectationHandler expectationHandler = expectationHandler();

        ValidatableResponse call_0 = given().accept("*/*")
                .get(baseUrlOfSut + "/api/basicResponsesNumeric/799")
                .then()
                .statusCode(200)
                .body(containsString("42"));
        expectationHandler.expect(true)
                .that(true, Arrays.asList(200, 401, 403, 404).contains(call_0.extract().statusCode()));

        ValidatableResponse call_1 = given().accept("*/*")
                .get(baseUrlOfSut + "/api/basicResponsesNumeric/-12")
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
