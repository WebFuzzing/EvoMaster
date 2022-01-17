package org.evomaster.e2etests.micronaut.rest;

import com.foo.micronaut.rest.MicronautTestController;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.hamcrest.core.Is.is;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class MicronautEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MicronautTestController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlaky("MicronautTest", "com.foo.MicronautTest", 10000, false, (args) -> {
            args.add("--killSwitch");
            args.add("false");

            Solution<RestIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);
            assertHasAtLeastOne(solution, HttpVerb.GET, 500);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200);
        } );

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        /*
            Below test checks for keep-alive header even when the server crashes.
        */

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get(baseUrlOfSut + "/")
                .then()
                .statusCode(500)
                .header("connection", is("keep-alive"))
                .body("message", is("Crashed"));
    }
}
