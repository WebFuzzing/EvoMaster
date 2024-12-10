package org.evomaster.e2etests.spring.web.malformedhtml;

import com.foo.web.examples.spring.malformedhtml.MalformedHtmlController;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MalformedHtmlEMTest extends SpringTestBase{

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new MalformedHtmlController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MalformedHtmlEM",
                "org.MalformedHtmlEM",
                20,
                (args) -> {

                    Solution<WebIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() > 0);

                    assertHasVisitedUrlPath(solution, "/malformedhtml/index.html","/malformedhtml/a.html");

                    //actually there is no fault found here, as Chrome is fixing the HTML
                }
        );
    }
}
