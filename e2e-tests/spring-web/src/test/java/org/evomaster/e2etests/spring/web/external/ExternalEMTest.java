package org.evomaster.e2etests.spring.web.external;

import com.foo.web.examples.spring.external.ExternalController;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ExternalController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ExternalEM",
                "org.ExternalEM",
                50,
                (args) -> {

                    Solution<WebIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() > 0);

                    assertHasVisitedUrlPath(solution, "/external/index.html", "/external/a.html");

                    //TODO add more checks
                }
        );
    }
}
