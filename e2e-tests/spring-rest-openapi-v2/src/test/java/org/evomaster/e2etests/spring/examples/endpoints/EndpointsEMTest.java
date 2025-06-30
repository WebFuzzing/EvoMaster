package org.evomaster.e2etests.spring.examples.endpoints;

import com.foo.rest.examples.spring.endpoints.EndpointsController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndpointsEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EndpointsController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EndpointsEM",
                "org.foo.EndpointsEM",
                500,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.OPTIONS, HttpStatus.OK.value(),"/api/endpoints/options",null);
                    assertHasAtLeastOne(solution, HttpVerb.HEAD, HttpStatus.OK.value(),"/api/endpoints/head",null);

                    //Swagger Parser has a bug, in which it ignores TRACE in V2. but works for V3
                    //assertHasAtLeastOne(solution, HttpVerb.TRACE, HttpStatus.OK.value(),"/api/endpoints/trace",null);

                });
    }
}