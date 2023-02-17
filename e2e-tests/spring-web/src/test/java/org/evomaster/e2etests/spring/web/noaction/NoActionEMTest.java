package org.evomaster.e2etests.spring.web.noaction;

import com.foo.web.examples.spring.noaction.NoActionController;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoActionEMTest extends SpringTestBase{

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new NoActionController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "NoActionEM",
                "org.NoActionEM",
                20,
                (args) -> {

                    //TODO disable back and refresh actions

                    Solution<WebIndividual> solution = initAndRun(args);

                    assertEquals(solution.getIndividuals().size(), 1);

                    assertHasVisitedUrlPath(solution, "/noaction/index.html");
                }
        );
    }
}
