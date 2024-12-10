package org.evomaster.e2etests.spring.examples.endpointfilter;

import com.foo.rest.examples.spring.endpointfilter.EndpointFilterController;
import com.foo.rest.examples.spring.endpoints.EndpointsController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndpointFilterEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EndpointFilterController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EndpointFilterEM",
                "org.foo.EndpointFilterEM",
                50,
                (args) -> {

                    args.add("--endpointTagFilter");
                    args.add("Foo");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    /*
                        6 endpoints + 1 of schema
                        - 4 excluded due to controller settings
                        - 1 excluded due to tag
                     */
                    assertEquals(2, solution.getIndividuals().size());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/endpointfilter/y/z", null);
                    assertNone(solution, HttpVerb.GET, 200, "/api/endpointfilter/y", null);
                });
    }
}