package org.evomaster.e2etests.micronaut.patio;

import com.foo.micronaut.patio.MicronautPatioTestController;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Disabled;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@Disabled
public class MicronautPatioEMTest extends RestTestBase {

    @BeforeClass
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MicronautPatioTestController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlaky("MicronautPatioTest", "com.foo.MicronautPatioTest", 100, false, (args) -> {
            args.add("--killSwitch");
            args.add("false");

            Solution<RestIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);
            assertHasAtLeastOne(solution, HttpVerb.GET, 500);
        } );

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        /*
            It is expected to have keep-alive in connection header to maintain
            the connection even the application crashes. Since the method implementation
            is not working properly, for the moment below test checks for close header
            value for connection.
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
