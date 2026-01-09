package org.evomaster.e2etests.spring.examples.endpointexclude;

import com.foo.rest.examples.spring.endpointexclude.EndpointExcludeController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointExcludeEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EndpointExcludeController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EndpointExcludeEM",
                "org.foo.EndpointExcludeEM",
                50,
                (args) -> {

                    args.add("--endpointExclude");
                    args.add("/api/endpointexclude/x,/api/endpointexclude/y");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/endpointexclude/y/z", null);
                    assertNone(solution, HttpVerb.GET, 200, "/api/endpointexclude/x", null);
                    assertNone(solution, HttpVerb.GET, 200, "/api/endpointexclude/y", null);
                });
    }
}