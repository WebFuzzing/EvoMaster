package org.evomaster.e2etests.spring.web.wronglinks;

import com.foo.web.examples.spring.alinks.ALinksController;
import com.foo.web.examples.spring.wronglinks.WrongLinksController;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WrongLinksEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new WrongLinksController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "WrongLinksEM",
                "org.WrongLinksEM",
                50,
                (args) -> {

                    Solution<WebIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() > 0);

                    assertHasVisitedUrlPath(solution, "/wronglinks/index.html", "/wronglinks/a.html", "/wronglinks/b.html");
                    assertHasAnyHtmlErrors(solution);
                }
        );
    }
}
