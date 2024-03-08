package org.evomaster.e2etests.spring.examples.authenticatedswaggeraccesstest;

import com.foo.rest.examples.spring.unauthenticatedswaggeraccesscontroller.UnauthenticatedSwaggerAccessController;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UnauthenticatedSwaggerAccessEMTest extends SpringTestBase {

    private static UnauthenticatedSwaggerAccessController controller;

    @BeforeAll
    public static void initClass() throws Exception {

        controller = new UnauthenticatedSwaggerAccessController();
        SpringTestBase.initClass(controller);
    }

    /**
     * Since the swagger endpoint is authenticated, it can be retrieved using authentication object.
     * @throws Throwable
     */
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "UnauthenticatedSwaggerAccessEM",
                "org.bar.UnauthenticatedSwaggerAccessEM",
                100,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                });
    }



}
