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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicronautPatioTest extends RestTestBase {

    @BeforeClass
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MicronautPatioTestController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlaky("MicronautPatioTest", "com.foo.MicronautPatioTest", 1000, false, (args) -> {
            args.add("--killSwitch");
            args.add("false");

            Solution<RestIndividual> solution = initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);
            assertHasAtLeastOne(solution, HttpVerb.GET, 500);
        } );

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
