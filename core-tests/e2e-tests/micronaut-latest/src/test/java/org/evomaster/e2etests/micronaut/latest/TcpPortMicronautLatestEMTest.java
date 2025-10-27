package org.evomaster.e2etests.micronaut.latest;

import com.foo.micronaut.latest.MicronautTestController;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpPortMicronautLatestEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MicronautTestController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlaky("TcpPortMicronautLatestEMTest", "com.foo.TcpPortMicronautLatestEMTest", 1000, true, (args) -> {
            args.add("--killSwitch");
            args.add("false");

            Solution<RestIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/", null);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/", null);
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/tcpPort", null);
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/tcpPortFailed", null);
        } );

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/tcpPort")
                .then()
                .statusCode(200)
                .body("size()", is(2)); // 1 from search, and 1 here from RestAssured

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
